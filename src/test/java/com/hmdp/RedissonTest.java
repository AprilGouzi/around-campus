package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.SystemConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.SystemConstants.SHOP_GEO_KEY;

/**
 * @author 囍崽
 * version 1.0
 */
@Slf4j
@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IShopService shopService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private RLock lock;

    @BeforeEach
    void setUp() {
        lock = redissonClient.getLock("order");
    }

    void method01() throws InterruptedException {
        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
        if (!isLock) {
            log.error("获取锁失败，1");
            return;
        }
        try {
            log.info("获取锁成功，1");
            method02();
        } finally {
            log.info("释放锁，1");
            lock.unlock();
        }
    }

    void method02() {
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("获取锁失败，2");
            return;
        }
        try {
            log.info("获取锁成功，2");
            method02();
        } finally {
            log.info("释放锁，2");
            lock.unlock();
        }
    }

    @Test
    void testSaveShop() {
        Shop shop = shopService.getById(2L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 2L, shop, 10L, TimeUnit.SECONDS);

    }

    @Test
    void loadShopData() {
        //1. 查询店铺信息
        List<Shop> list = shopService.list();
        //2. 把店铺分组，按照typeId 分组
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //3.分批完成写入 redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {

            //3.1 获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            //3.2 获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());

            //3.3 写入redis
            for (Shop shop : value) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }

    }
}
