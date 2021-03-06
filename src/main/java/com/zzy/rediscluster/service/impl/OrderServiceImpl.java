package com.zzy.rediscluster.service.impl;

import com.zzy.rediscluster.service.OrderService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * @Page com.zzy.rediscluster.service.impl
 * @Author Sevon
 * @Date 2020/3/7 20:59
 * @Version 1.0
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private Redisson redisson;

    /**
     * 库存操作
     *
     * @param productId
     * @return
     */
    @Override
    public String createOrder(String productId) {
        String lockKey = productId + "LOCK";
//        String clientId = UUID.randomUUID().toString();

        RLock redissonLock = redisson.getLock(lockKey);
        try {
           /* Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, clientId, 30, TimeUnit.SECONDS);
            if (!result) {
                return "FALSE";
            }*/

            //加锁  实现锁续命
            redissonLock.lock();
            int stock = Integer.parseInt(Objects.requireNonNull(stringRedisTemplate.opsForValue().get(productId)));
            if (stock > 0) {
                int resStock = stock - 1;
                stringRedisTemplate.opsForValue().set(productId, resStock + "");
                System.out.println("扣减库存成功,剩余库存" + resStock);
                logger.info("扣减库存成功,剩余库存" + resStock);
            } else {
                logger.info("库存不足");
                System.out.println("库存不足，扣减库存失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("扣减库存失败：" + e);
            return "FALSE";
        } finally {
            //释放锁
            redissonLock.unlock();
            /*if (clientId.equals(stringRedisTemplate.opsForValue().get(lockKey))) {
                stringRedisTemplate.delete(lockKey);
            }*/
        }
        return "SUCCESS";
    }
}
