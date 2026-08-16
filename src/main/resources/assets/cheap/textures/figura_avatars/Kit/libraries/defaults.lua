-- Just declares default values for config

return {
    default = function(key, value)
        if config:load(key) == nil then
            config:save(key, value)
        end
    end
}
