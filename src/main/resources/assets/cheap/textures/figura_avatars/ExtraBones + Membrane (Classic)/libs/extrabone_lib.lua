-- ## extrabone_lib -> Emotecraft Support Library ##

-- This script only supports Figura with the ExtraBone Addon and cannot be used outside of that Addon.
-- If you're interested, you can download the mod at: https://modrinth.com/mod/figura_extrabone
-- For more information, contact Discord: kafunech

local boneList = {}
local function ExtraBoneInit(list)
    boneList = list
end

events.RENDER:register(function(delta)
    local uuid = player:getUUID()
    if client:isModLoaded("figuraextrabone") then
        for key, value in pairs(boneList) do
            value[1]:rot(vec((ExtraBone.getBone(uuid,value[2])[2] * (180/math.pi)) * -1, 0, 0))
        end
    end
end)

return ExtraBoneInit
