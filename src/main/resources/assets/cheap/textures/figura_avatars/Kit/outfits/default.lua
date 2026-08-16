local configDef = require "libraries.defaults".default
local model = models.default

-- Pings, Configs and Utils

configDef("default.legacy", false)
configDef("default.goggles", false)

local legacy = {
    model.root.LeftArm.LeftShoulderOld,
    model.root.RightArm.RightShoulderOld,
    model.root.LeftLeg.LeftThighOld,
    model.root.RightLeg.RightThighOld
}

function pings.default_legacy(bool)
    for _, v in pairs(legacy) do
        v:setVisible(bool)
    end
    config:save("default.legacy", bool)
end

function pings.default_goggles(bool)
    model.root.Head.Goggles:setVisible(not bool)
    model.root.Head.GogglesOn:setVisible(bool)
    config:save("default.goggles", bool)
end

pings.default_legacy(config:load("default.legacy"))
pings.default_goggles(config:load("default.goggles"))

-- Action wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Toggle Goggles")
    :item("glass_pane")
    :toggleItem("magenta_stained_glass_pane")
    :toggleColor(vectors.hexToRGB("B10FC3"))
    :onToggle(pings.default_goggles)
    :toggled(config:load("default.goggles"))
    
page:newAction()
    :title("Legacy Design")
    :item("black_concrete")
    :toggleItem("pink_concrete")
    :toggleColor(vectors.hexToRGB("E510B3"))
    :onToggle(pings.default_legacy)
    :toggled(config:load("default.legacy"))


return {
    name = "default",
    model = model,
    animations = animations.default,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Regular Outfit",
        item = "golden_apple",
        -- toggleItem = "apple",
        color = "FFFFC800"
    }
}
