local configDef = require "libraries.defaults".default
local model = models.swimsuit

-- Pings, Configs and Utils

configDef("swimsuit.goggles", false)

function pings.swimsuit_goggles(bool)
    model.root.Head.Goggles:setVisible(not bool)
    model.root.Head.GogglesOn:setVisible(bool)
    config:save("swimsuit.goggles", bool)
end

pings.swimsuit_goggles(config:load("swimsuit.goggles"))

-- Action wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Toggle Goggles")
    :item("glass_pane")
    :toggleItem("magenta_stained_glass_pane")
    :toggleColor(vectors.hexToRGB("B10FC3"))
    :onToggle(pings.swimsuit_goggles)
    :toggled(config:load("swimsuit.goggles"))

return {
    name = "swimsuit",
    model = model,
    animations = animations.swimsuit,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Swimsuit Kit",
        item = "cod",
        toggleItem = "tropical_fish",
        -- color = "FFE4D209",
        toggleColor = "FFE9FF27"
    }
}
