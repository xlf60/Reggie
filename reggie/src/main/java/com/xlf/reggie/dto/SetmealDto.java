package com.xlf.reggie.dto;

import com.xlf.reggie.entity.Setmeal;
import com.xlf.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
