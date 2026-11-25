-- Compare-and-set cache populate with version monotonicity.
-- Only writes if: key absent OR new version > existing version.
-- A tombstone at version N blocks any stale write with version <= N.
--
-- KEYS[1] = cache key
-- ARGV[1] = JSON value to store
-- ARGV[2] = version (number)
-- ARGV[3] = TTL in seconds (optional, default 3600)
-- Returns: 1 if written, 0 if rejected (stale)

local existing = redis.call('GET', KEYS[1])
if existing then
    local ok, data = pcall(cjson.decode, existing)
    if ok and data and tonumber(data.version) then
        if tonumber(ARGV[2]) <= tonumber(data.version) then
            return 0
        end
    end
end
local ttl = tonumber(ARGV[3]) or 3600
redis.call('SET', KEYS[1], ARGV[1], 'EX', ttl)
return 1
