package com.hmdp.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.SystemConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author XIZAI
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询店铺类型
     *
     * @return
     */
    @Override
    public Result queryTypeList() {
        //1. 从Redis 中查询店铺类型
        String key = CACHE_SHOP_TYPE_KEY + UUID.randomUUID().toString(true);
        List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(key, 0, -1);
        //判断Redis 中是否有该缓存，直接返回
        if (shopTypeJsonList != null && !shopTypeJsonList.isEmpty()) {
            ArrayList<ShopType> typeList = new ArrayList<>();
            for (String str : shopTypeJsonList) {
                typeList.add(JSONUtil.toBean(str, ShopType.class));
            }
            return Result.ok(typeList);
        }

        //2.2  Redis 中若不存在该数据，则从数据库中查询
        List<ShopType> typeList = query().orderByAsc("sort").list();

        //3. 判断数据库是否存在
        if (typeList == null || typeList.isEmpty()){
            //3.1 数据库中也不存在，则返回list
            return Result.fail("分类不存在！");
        }

        //3.2 数据库中存在，则将查询到的信息存入Redis
        for (ShopType shopType : typeList) {
            stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY,JSONUtil.toJsonStr(shopType));
        }

        return Result.ok(typeList);
    }

    @Override
    public Result queryTypeString() {
        // 1. 从redis 中查询店铺缓存
        String shopTypeJSON = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);

        List<ShopType> shopTypes = null;
        //2. 判断缓存是否命中
        if (StrUtil.isNotBlank(shopTypeJSON)) {
            //2.1 缓存名中国，直接返回缓存数据
            shopTypes = JSONUtil.toList(shopTypeJSON, ShopType.class);
            return Result.ok(shopTypes);
        }
        //2.1 缓存未命中,查询数据库
        shopTypes = this.list(new LambdaQueryWrapper<ShopType>().orderByAsc(ShopType::getSort));
        //3. 判断数据库中是否存在该数据
        if (Objects.isNull(shopTypes)) {
            //3.1 数据库中不存在该数据，返回失败信息
            return Result.fail("店铺类型不存在");
        }
        //3.2 店铺数据存在，写入Redis ，并返回查询的数据
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopTypes), CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        return Result.ok(shopTypes);
    }
}
