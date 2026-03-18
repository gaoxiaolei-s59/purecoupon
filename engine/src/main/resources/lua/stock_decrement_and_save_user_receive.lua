-- Lua 脚本: 检查用户是否达到优惠券领取上限并记录领取次数

-- KEYS[1]: 优惠券库存键
-- KEYS[2]: 用户领取记录键
-- ARGV[1]: 用户领取记录过期时间（秒）
-- ARGV[2]: 用户领取上限

-- 返回:
-- {0, count} 成功，count 为领取后的次数
-- {1, 0}     库存不足
-- {2, count} 达到上限，count 为当前已领取次数

local stock = tonumber(redis.call('HGET', KEYS[1], 'stock'))
if stock == nil or stock <= 0 then
    return {1, 0}
end

local userCouponCount = tonumber(redis.call('GET', KEYS[2]))
if userCouponCount == nil then
    userCouponCount = 0
end

local limit = tonumber(ARGV[2])
if userCouponCount >= limit then
    return {2, userCouponCount}
end

if userCouponCount == 0 then
    redis.call('SET', KEYS[2], 1)
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[1]))
else
    redis.call('INCR', KEYS[2])
end

redis.call('HINCRBY', KEYS[1], 'stock', -1)

return {0, userCouponCount + 1}