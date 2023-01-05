package com.xlf.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xlf.reggie.common.BaseContext;
import com.xlf.reggie.common.R;
import com.xlf.reggie.dto.OrdersDto;
import com.xlf.reggie.entity.OrderDetail;
import com.xlf.reggie.entity.Orders;
import com.xlf.reggie.entity.ShoppingCart;
import com.xlf.reggie.service.OrderDetailService;
import com.xlf.reggie.service.OrderService;
import com.xlf.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;
    /**
     * 修改订单状态
     * @param orders 订单信息
     * @return
     */
    @PutMapping
    public R<String> status(@RequestBody Orders orders){
        orderService.updateById(orders);
        return  R.success("修改成功");
    }
    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }


    /**
     * 订单信息分页查询
     * @param page 第几页
     * @param pageSize  分页数据
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime,@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime){
        log.info(number + "==="+beginTime + "==="+endTime);

        //构造分页构造器对象
        Page<Orders> pageInfo = new Page<>(page,pageSize);

        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();

        //添加过滤条件
        queryWrapper.like(number != null, Orders::getNumber,number)
                .between(beginTime != null && endTime != null,Orders::getCheckoutTime,beginTime,endTime);


        //添加排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);

        //执行分页查询
        orderService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);
    }

    @GetMapping("/userPage")
    public R<Page> page(int page, int pageSize) {
        //获取当前id
        Long userId = BaseContext.getCurrentId();
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //查询当前用户id订单数据
        queryWrapper.eq(userId != null, Orders::getUserId, userId);
        //按时间降序排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo, queryWrapper);
        List<OrdersDto> list = pageInfo.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            //获取orderId,然后根据这个id，去orderDetail表中查数据
            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> details = orderDetailService.list(wrapper);
            BeanUtils.copyProperties(item, ordersDto);
            //之后set一下属性
            ordersDto.setOrderDetails(details);
            return ordersDto;
        }).collect(Collectors.toList());
        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
        ordersDtoPage.setRecords(list);
        //日志输出看一下
        log.info("list:{}", list);
        return R.success(ordersDtoPage);
    }

    /**
     * 再来一单
     * @param map
     * @return
     */
    @PostMapping("/again")
    public R<String> again(@RequestBody Map<String,String> map){
        //获取order_id
        Long orderId = Long.valueOf(map.get("id"));
        //条件构造器
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        //查询订单的口味细节数据
        queryWrapper.eq(OrderDetail::getOrderId,orderId);
        List<OrderDetail> details = orderDetailService.list(queryWrapper);
        //获取用户id，待会需要set操作
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = details.stream().map((item) ->{
            ShoppingCart shoppingCart = new ShoppingCart();
            //Copy对应属性值
            BeanUtils.copyProperties(item,shoppingCart);
            //设置一下userId
            shoppingCart.setUserId(userId);
            //设置一下创建时间为当前时间
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        //加入购物车
        shoppingCartService.saveBatch(shoppingCarts);
        return R.success("喜欢吃就再来一单吖~");
    }


}