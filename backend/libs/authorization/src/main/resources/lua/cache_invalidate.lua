-- Write a tombstone and publish invalidation event.
-- Tombstone prevents stale readers from repopulating with old data.
-- Version monotonicity is the correctness guarantee; TTL is cleanup.
--
-- KEYS[1] = cache key
-- ARGV[1] = version (number)
-- ARGV[2] = tombstone TTL in seconds
-- ARGV[3] = invalidation channel name
-- ARGV[4] = invalidation event JSON payload
-- Returns: 1 always

local tombstone = cjson.encode({_tombstone = true, version = tonumber(ARGV[1])})
redis.call('SET', KEYS[1], tombstone)
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
redis.call('PUBLISH', ARGV[3], ARGV[4])
return 1
