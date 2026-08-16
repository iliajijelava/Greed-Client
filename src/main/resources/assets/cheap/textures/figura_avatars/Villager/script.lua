require("select")
vanilla_model.PLAYER:setVisible(false)
vanilla_model.CAPE:setVisible(false)
nameplate.ENTITY:setVisible(false)

local skinBiome = "plains"
local skinProfession = nil
local skinLevel = "stone"

local skin = textures:fromVanilla("skin", "minecraft:textures/entity/villager/villager.png")
models.model.root:setPrimaryTexture("CUSTOM", skin)

local function compileSkin(biome, profession, level)
    skin:restore()
    if biome then
        local b = textures:fromVanilla("biome", "minecraft:textures/entity/villager/type/"..biome..".png")
        for y = 0, skin:getDimensions().y-1 do
            for x = 0, skin:getDimensions().x-1 do
                if b:getPixel(x, y).a ~= 0 then
                    skin:setPixel(x, y, b:getPixel(x, y))
                end
            end
        end
        skin:update()
    end
    if profession then
        local p = textures:fromVanilla("profession", "minecraft:textures/entity/villager/profession/"..profession..".png")
        for y = 0, skin:getDimensions().y-1 do
            for x = 0, skin:getDimensions().x-1 do
                if p:getPixel(x, y).a ~= 0 then
                    skin:setPixel(x, y, p:getPixel(x, y))
                end
            end
        end
        skin:update()
    end
    if level then
        local l = textures:fromVanilla("level", "minecraft:textures/entity/villager/profession_level/"..level..".png")
        for y = 0, skin:getDimensions().y-1 do
            for x = 0, skin:getDimensions().x-1 do
                if l:getPixel(x, y).a ~= 0 then
                    skin:setPixel(x, y, l:getPixel(x, y))
                end
            end
        end
        skin:update()
    end
    return skin
end

function pings.updateSkin(biome, profession, level)
    compileSkin(biome, profession, level)
end

if host:isHost() then
    local main = action_wheel:newPage()
    action_wheel:setPage(main)

    local biomeAction = main:newSelect():title("Biome"):item("minecraft:grass_block"):setOnLeftClick(function (self, value)
        skinBiome = value
        pings.updateSkin(skinBiome, skinProfession, skinLevel)
    end)
    for _, biome in ipairs(client.getRegistry("villager_type")) do
        local namespace, id = biome:match("^([^:]+):(.+)$")
        biomeAction:addChoice(client.getTranslatedString("biome."..namespace.."."..id), id)
    end

    local professionAction = main:newSelect():title("Profession"):item("minecraft:crafting_table"):setOnLeftClick(function (self, value)
        if value == "none" then
            skinProfession = nil
        else
            skinProfession = value
        end
        pings.updateSkin(skinBiome, skinProfession, skinLevel)
    end)
    for _, profession in ipairs(client.getRegistry("villager_profession")) do
        local namespace, id = profession:match("^([^:]+):(.+)$")
        professionAction:addChoice(client.getTranslatedString("entity."..namespace..".villager."..id), id)
    end

    local levelAction = main:newSelect():title("Level"):item("minecraft:gold_nugget"):setOnLeftClick(function (self, value)
        skinLevel = value
        pings.updateSkin(skinBiome, skinProfession, skinLevel)
    end)
    for i, level in ipairs({"stone", "iron", "gold", "emerald", "diamond"}) do
        levelAction:addChoice(client.getTranslatedString("merchant.level."..i), level)
    end
end