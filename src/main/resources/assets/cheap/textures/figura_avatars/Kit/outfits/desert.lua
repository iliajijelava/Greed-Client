local configDef = require "libraries.defaults".default
local outfitAPI = require "libraries.outfitAPI"
local model = models.desert

-- -- Pings, Config and Utils

configDef("desert.bow", true)
configDef("desert.goggles", false)

function pings.desert_bow(bool)
    model.root.Body.Bow:setVisible(bool)
    config:save("desert.bow", bool)
end

function pings.desert_goggles(bool)
    model.root.Head.Goggles:setVisible(not bool)
    model.root.Head.GogglesOn:setVisible(bool)
    config:save("desert.goggles", bool)
end

pings.desert_bow(config:load("desert.bow"))
pings.desert_goggles(config:load("desert.goggles"))

-- -- Action Wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Toggle Bow on back")
    :item("arrow")
    :toggleItem("bow")
    :toggleColor(vectors.hexToRGB("A05505"))
    :toggled(config:load("desert.bow"))
    :onToggle(pings.desert_bow)

page:newAction()
    :title("Toggle Goggles")
    :item("glass_pane")
    :toggleItem("pink_stained_glass_pane")
    :toggleColor(vectors.hexToRGB("DA27C8"))
    :toggled(config:load("desert.goggles"))
    :onToggle(pings.desert_goggles)

-- -- Events

-- Hide bow on your back if you hold one
local function isHoldingBow()
    local main, offhand = player:getHeldItem(), player:getHeldItem(true)
    return main.id == "minecraft:bow" or offhand.id == "minecraft:bow"
end

local wasHoldingBow = false
function events.TICK()
    -- Quit if outfit not selected
    if outfitAPI.selected ~= "desert" then return end

    local holdingBow = isHoldingBow()
    -- Uhhh no clue why these bools inverted but who tf cares
    if holdingBow and not wasHoldingBow then
        model.root.Body.Bow.Bow2:setVisible(false)
        wasHoldingBow = true
    elseif not holdingBow and wasHoldingBow then
        model.root.Body.Bow.Bow2:setVisible(true)
        wasHoldingBow = false
    end
end

return {
    name = "desert",
    model = models.desert,
    animations = animations.desert,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Deser Voyager/Medieval Kit",
        item = "sand",
        toggleItem = "red_sandstone",
        -- color = "FFdd484c",
        toggleColor = "FFEEFD7B"
    }
}
