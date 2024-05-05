package com.hmdp.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author XIZAI
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("list")
    public Result queryTypeList() {
        //String key = "cache:shopType";
        ////查找内存
        //String typeJson = stringRedisTemplate.opsForValue().get(key);
        //if (StrUtil.isBlank(typeJson)){
        //    List<ShopType> typeList = typeService
        //            .query().orderByAsc("sort").list();
        //    stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(typeList));
        //    return Result.ok(typeList);
        //}
        //List<ShopType> list = JSONUtil.toList(typeJson, ShopType.class);
        return typeService.queryTypeList();
    }
}
