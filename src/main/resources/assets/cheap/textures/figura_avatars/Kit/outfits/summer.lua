local configDef = require "libraries.defaults".default
local model = models.summer

-- Pings, Configs and Utils

configDef("summer.goggles", false)
configDef("summer.hat", false)

function pings.summer_goggles(bool)
    model.root.Head.Sunglasses:setVisible(not bool)
    model.root.Head.SunglassesOn:setVisible(bool)
    config:save("summer.goggles", bool)
end

function pings.summer_hat(bool)
    model.root.Body.Beach_Hat2:setVisible(not bool)
    model.root.Head.Ears:setVisible(not bool)
    model.root.Head.Beach_Hat:setVisible(bool)
    config:save("summer.hat", bool)
end

pings.summer_goggles(config:load("summer.goggles"))
pings.summer_hat(config:load("summer.hat"))

-- Action wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Toggle Sunglasses")
    :item("black_stained_glass_pane")
    :toggleItem("magenta_stained_glass_pane")
    :toggleColor(vectors.hexToRGB("B10FC3"))
    :onToggle(pings.summer_goggles)
    :toggled(config:load("summer.goggles"))

page:newAction()
    :title("Use Beach Hat")
    :item("wheat")
    :toggleItem("hay_block")
    :toggleColor(vectors.hexToRGB("F6C94F"))
    :onToggle(pings.summer_hat)
    :toggled(config:load("summer.hat"))


return {
    name = "summer",
    model = model,
    animations = animations.default,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Summer Kit",
        item = "cyan_wool",
        toggleItem = "wheat",
        -- color = "FFE4D209",
        toggleColor = "FF00B3FF"
    }
}
