local configDef = require "libraries.defaults".default
local outfitAPI = require "libraries.outfitAPI"
local model = models.soldier

-- -- Pings, Config and Utils

configDef("soldier.toggleGun", false)
configDef("soldier.LeftGun", false)
configDef("soldier.replaceGun", true)
local toggledGun = config:load("soldier.toggleGun")
local leftGun = config:load("soldier.LeftGun")
local replaceGun = config:load("soldier.replaceGun")


function pings.soldier_toggleGun(bool, offhand)
    if player:isLoaded() and player:isLeftHanded() then
        offhand = not offhand
    end

    if not offhand then
        model.root.RightArm.RightForearm.RightRealArm:setVisible(not bool)
        model.root.RightArm.RightForearm.RightFireArm:setVisible(bool)
    else
        model.root.LeftArm.LeftForearm.LeftRealArm:setVisible(not bool)
        model.root.LeftArm.LeftForearm.LeftFireArm:setVisible(bool)
    end
    config:save("soldier.toggleGun", bool)
end

function pings.soldier_replaceGun(bool)
    replaceGun = bool
    config:save("soldier.replaceGun", bool)
end

pings.soldier_toggleGun(config:load("soldier.toggleGun"), config:load("LeftGun"))


local function hideRevolvers()
    pings.soldier_toggleGun(false)
    pings.soldier_toggleGun(false, true)
end

-- -- Action Wheel

local page = action_wheel:newPage()

local gunAct = page:newAction()
    :title("Toggle SMG")
    :item("bow")
    :toggleItem("crossbow")
    :toggleColor(vectors.hexToRGB("4C4C4C"))
    :toggled(config:load("soldier.toggleGun"))

local leftHandAct = page:newAction()
    :title("Use Offhand")
    :item("grass_block")
    :toggleItem("mycelium")
    :toggleColor(vectors.hexToRGB("AC5CE9"))
    :toggled(config:load("soldier.LeftGun"))

local replaceGunAct = page:newAction()
    :title("Replace bow with SMG")
    :item("spectral_arrow")
    :toggleItem("tnt")
    :toggleColor(vectors.hexToRGB("26600D"))
    :toggled(config:load("soldier.replaceGun"))

-- Toggle shennanigans
gunAct:onToggle(function(bool)
    -- Disable bow thingamajig
    pings.soldier_replaceGun(false)
    replaceGunAct:toggled(false)

    toggledGun = bool
    hideRevolvers()
    pings.soldier_toggleGun(toggledGun, leftGun)
    config:save("soldier.toggleGun", bool)
    config:save("soldier.replaceGun", false)
end)

leftHandAct:onToggle(function(bool)
    -- Disable bow thingamajig
    pings.soldier_replaceGun(false)
    replaceGunAct:toggled(false)

    leftGun = bool
    hideRevolvers()
    pings.soldier_toggleGun(toggledGun, leftGun)
    config:save("soldier.LeftGun", bool)
    config:save("soldier.replaceGun", false)
end)

replaceGunAct:onToggle(function(bool)
    -- Disable Current revolvers
    pings.soldier_toggleGun(false, false)
    pings.soldier_toggleGun(false, true)
    gunAct:toggled(false)

    pings.soldier_replaceGun(bool)
    config:save("soldier.replaceGun", bool)
    config:save("soldier.toggleGun", false)
end)

-- -- Events

-- System behind rendering the gun
-- Gun ids:
local gunOverrides = {
    "minecraft:crossbow",
    "minecraft:bow"
}
-- Format it so lua can read it easier
local gunFormatted = {}
for _, v in pairs(gunOverrides) do
    gunFormatted[v] = true
end

-- Hide the gun item from non hosts
if not host:isHost() then
    model.ItemFireArm:setVisible(false)
end

-- Checks and updates every frame if player is in first person
local firstPerson = false
events.RENDER:register(function (_, ctx) firstPerson = ctx == "FIRST_PERSON" end)

local function getBowHand()
    if not player:isLoaded() then return false, false end
    local main, offhand = player:getHeldItem(), player:getHeldItem(true)
    return gunFormatted[main.id], gunFormatted[offhand.id]
end

-- Core of the whole system
local lastRight, lastLeft = false, false
function events.item_render(item)
    -- Quit if outfit unloaded
    if not replaceGun then return end
    if outfitAPI.selected ~= "soldier" then return end

    -- When item not detected then quit
    if not gunFormatted[item.id] then
        -- Disable the models
        if lastRight or lastLeft then
            pings.soldier_toggleGun(false)
            pings.soldier_toggleGun(false, true)
            lastRight, lastLeft = false, false
        end
        return
    end

    -- Get The hands the bows are on
    local main, offhand = getBowHand()

    -- Do dominant hand stuff
    if main and not lastRight then
        pings.soldier_toggleGun(true)
        lastRight = true
    elseif not main and lastRight then
        pings.soldier_toggleGun(false)
        lastRight = false
    end
    
    -- Offhand stuff
    if offhand and not lastLeft then
        pings.soldier_toggleGun(true, true)
        lastLeft = true
    elseif not offhand and lastLeft then
        pings.soldier_toggleGun(false, true)
        lastLeft = false
    end

    -- Make sure the gun item model only renders for first person (aesthetic reasons)
    if firstPerson then
        return model.ItemSMG
    else
        return model.ItemInvisible
    end
end

-- Unload gun after switching to air
function events.TICK()
    if not replaceGun then return end
    
    local main, offhand = getBowHand()
    -- hide main hand
    if not main and lastRight then
        pings.soldier_toggleGun(false)
        lastRight = false
    end

    -- hide offhand
    if not offhand and lastLeft then
        pings.soldier_toggleGun(false, true)
        lastLeft = false
    end
end


return {
    name = "soldier",
    model = models.soldier,
    animations = animations.soldier,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Soldier Kit",
        item = "flint_and_steel",
        toggleItem = "tnt",
        -- color = "FF81541E",
        toggleColor = "FF37920F"
    }
}
