package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 缓存重建线程池
     */
    public static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 根据 id 查询商铺数据
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        Shop shop = cacheClient.handleCacheBreakdown(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);
        if (Objects.isNull(shop)) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);

        //String key = CACHE_SHOP_KEY + id;
        ////1.从redis 查询商铺缓存
        //String shopJson = stringRedisTemplate.opsForValue().get(key);
        //
        ////判断是否存在
        //if (StrUtil.isBlank(shopJson)) {
        //    //1.1 缓存未命中
        //    return Result.fail("店铺不存在");
        //}
        ////1.2 缓存命中，将JSON字符串反序列化对象，并判断缓存数据是否逻辑过期
        //RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        ////这里需要先转成JSONObject 再转成反序列化,否则可能无法正确映射Shop字段
        //JSONObject data =(JSONObject) redisData.getData();
        //Shop shop = JSONUtil.toBean(data, Shop.class);
        //LocalDateTime expireTime = redisData.getExpireTime();
        //if (expireTime.isAfter(LocalDateTime.now())){
        //    //当前缓存数据未过期，直接返回
        //    return Result.ok(shop);
        //}
        ////2.缓存数据已过期，获取互斥锁，并且重建缓存
        //String lockKey = LOCK_SHOP_KEY + id;
        //boolean isLock = tryLock(lockKey);
        //if (isLock){
        //    //获取锁成功，开启一个子线程去重建缓存
        //    CACHE_REBUILD_EXECUTOR.submit(()->{
        //        try {
        //            this.saveShopToCache(id,CACHE_SHOP_TTL);
        //        } finally {
        //            unlock(lockKey);
        //        }
        //    });
        //}
        //
        ////3.获取锁失败，再次查询缓存，判断缓存是否重建（DOUBLE CHECK
        //shopJson = stringRedisTemplate.opsForValue().get(key);
        //if (StrUtil.isBlank(shopJson)){
        //    //3.1 缓存未命中，直接返回失败信息
        //    return Result.fail("店铺数据不存在");
        //}
        //
        ////3.2 缓存命中，将JSON 字符串反序列化对象，并判断数据是否逻辑过期
        //redisData = JSONUtil.toBean(shopJson,RedisData.class);
        //data = (JSONObject) redisData.getData();
        //shop = JSONUtil.toBean(data,Shop.class);
        //expireTime = redisData.getExpireTime();
        //if (expireTime.isAfter(LocalDateTime.now())){
        //    //当前缓存数据未过期，直接返回
        //    return Result.ok(shop);
        //}
        //return Result.ok(shop);
    }

    //@Override
    //public Result queryById(Long id) {
    //    String key = CACHE_SHOP_KEY + id;
    //    //从redis 查询商铺缓存
    //    String shopJson = stringRedisTemplate.opsForValue().get(key);
    //
    //    Shop shop = null;
    //    //判断是否存在
    //    if (StrUtil.isNotBlank(shopJson)) {
    //        //存在，直接返回
    //        shop = JSONUtil.toBean(shopJson, Shop.class);
    //        return Result.ok(shop);
    //    }
    //    //2. 缓存未命中，判断缓存中查询的数据是否是空字符串
    //    if (Objects.nonNull(shopJson)) {
    //        return Result.fail("店铺不存在");
    //    }
    //
    //    //不存在，根据id 查询数据库
    //    shop = getById(id);
    //    //不存在，返回错误
    //    if (shop == null) {
    //        //数据库中不存在，缓存空对象（解决缓存穿透），返回失败信息
    //        stringRedisTemplate.opsForValue().set(key, "", CACHE_SHOP_TTL, TimeUnit.SECONDS);
    //        return Result.fail("店铺不存在！");
    //    }
    //    //数据库中存在，重建缓存，并返回店铺数据
    //    stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //    return Result.ok(shop);
    //}

    /**
     * 从缓存中获取店铺数据
     *
     * @param key
     * @return
     */
    private Result getShopFormCache(String key) {
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断缓存是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            //缓存数据有值，说明缓存命中，直接返回店铺数据
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //判断缓存中查询的数据是否空字符串，isNotBlank 把null 和 空字符串给排除了
        if (Objects.nonNull(shopJson)) {
            //当前数据是空字符串,说明缓存也命中了，（该数据是之前缓存的空对象） ，直接返回失败信息
            return Result.fail("店铺不存在");
        }
        //缓存未命中（缓存数据即没有值，又不是空字符串
        return null;
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //跟新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //1. 判断是否需要根据坐标查询
        if (x == null || y == null) {
            //不需要坐标查询，按数据库查询
            Page<Shop> page = query().eq("type_id", typeId)
                    .page(new Page<>(current, DEFAULT_PAGE_SIZE));
            //返回数据
            return Result.ok(page.getRecords());
        }

        //2. 计算分页参数
        int from = (current - 1) * DEFAULT_PAGE_SIZE;
        int end = current * DEFAULT_PAGE_SIZE;

        //3. 查询redis 、 按照距离排序，分页。结果：shopId,distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(key,
                GeoReference.fromCoordinate(x, y), new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));

        //4. 解析出 id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list =
                results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        //4.1 截取 from ~end 部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            //4.2 获取店铺 id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            //4.3 获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });

        //根据id 查询shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("order by field(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        return Result.ok(shops);
    }

    public void saveShopToCache(Long id, Long expirSeconds) {
        //从数据库中查询店铺数据
        Shop shop = this.getById(id);
        //封装逻辑过期数据
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expirSeconds));
        //将逻辑过期数据存入redis 中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 获取锁
     *
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
