local configDef = require "libraries.defaults".default
local outfitAPI = require "libraries.outfitAPI"
local model = models.cowgirl

-- -- Pings, Config and Utils

configDef("cowgirl.toggleGun", false)
configDef("cowgirl.LeftGun", false)
configDef("cowgirl.replaceGun", true)
local toggledGun = config:load("cowgirl.toggleGun")
local leftGun = config:load("conwgirl.LeftGun")
local replaceGun = config:load("cowgirl.replaceGun")


function pings.cowgirl_toggleGun(bool, offhand)
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
    config:save("cowgirl.toggleGun", bool)
end

function pings.cowgirl_replaceGun(bool)
    replaceGun = bool
    config:save("cowgirl.replaceGun", bool)
end

pings.cowgirl_toggleGun(config:load("cowgirl.toggleGun"), config:load("LeftGun"))


local function hideRevolvers()
    pings.cowgirl_toggleGun(false)
    pings.cowgirl_toggleGun(false, true)
end

-- -- Action Wheel

local page = action_wheel:newPage()

local gunAct = page:newAction()
    :title("Toggle Gun")
    :item("bow")
    :toggleItem("crossbow")
    :toggleColor(vectors.hexToRGB("00F2FF"))
    :toggled(config:load("cowgirl.toggleGun"))

local leftHandAct = page:newAction()
    :title("Use Offhand")
    :item("grass_block")
    :toggleItem("mycelium")
    :toggleColor(vectors.hexToRGB("AC5CE9"))
    :toggled(config:load("cowgirl.LeftGun"))

local replaceGunAct = page:newAction()
    :title("Replace bow with Revolver")
    :item("arrow")
    :toggleItem("spectral_arrow")
    :toggleColor(vectors.hexToRGB("FF0000"))
    :toggled(config:load("cowgirl.replaceGun"))

-- Toggle shennanigans
gunAct:onToggle(function(bool)
    -- Disable bow thingamajig
    pings.cowgirl_replaceGun(false)
    replaceGunAct:toggled(false)

    toggledGun = bool
    hideRevolvers()
    pings.cowgirl_toggleGun(toggledGun, leftGun)
    config:save("cowgirl.toggleGun", bool)
    config:save("cowgirl.replaceGun", false)
end)

leftHandAct:onToggle(function(bool)
    -- Disable bow thingamajig
    pings.cowgirl_replaceGun(false)
    replaceGunAct:toggled(false)

    leftGun = bool
    hideRevolvers()
    pings.cowgirl_toggleGun(toggledGun, leftGun)
    config:save("cowgirl.LeftGun", bool)
    config:save("cowgirl.replaceGun", false)
end)

replaceGunAct:onToggle(function(bool)
    -- Disable Current revolvers
    pings.cowgirl_toggleGun(false, false)
    pings.cowgirl_toggleGun(false, true)
    gunAct:toggled(false)

    pings.cowgirl_replaceGun(bool)
    config:save("cowgirl.replaceGun", bool)
    config:save("cowgirl.toggleGun", false)
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
    if outfitAPI.selected ~= "cowgirl" then return end

    -- When item not detected then quit
    if not gunFormatted[item.id] then
        -- Disable the models
        if lastRight or lastLeft then
            pings.cowgirl_toggleGun(false)
            pings.cowgirl_toggleGun(false, true)
            lastRight, lastLeft = false, false
        end
        return
    end

    -- Get The hands the bows are on
    local main, offhand = getBowHand()

    -- Do dominant hand stuff
    if main and not lastRight then
        pings.cowgirl_toggleGun(true)
        lastRight = true
    elseif not main and lastRight then
        pings.cowgirl_toggleGun(false)
        lastRight = false
    end
    
    -- Offhand stuff
    if offhand and not lastLeft then
        pings.cowgirl_toggleGun(true, true)
        lastLeft = true
    elseif not offhand and lastLeft then
        pings.cowgirl_toggleGun(false, true)
        lastLeft = false
    end

    -- Make sure the gun item model only renders for first person (aesthetic reasons)
    if firstPerson then
        return model.ItemFireArm
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
        pings.cowgirl_toggleGun(false)
        lastRight = false
    end

    -- hide offhand
    if not offhand and lastLeft then
        pings.cowgirl_toggleGun(false, true)
        lastLeft = false
    end
end


return {
    name = "cowgirl",
    model = models.cowgirl,
    animations = animations.cowgirl,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Cowgirl Kit",
        item = "bow",
        toggleItem = "crossbow",
        -- color = "FF81541E",
        toggleColor = "FFDFA30B"
    }
}
