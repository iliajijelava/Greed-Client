local configDef = require "libraries.defaults".default
local model = models.fighter

-- -- Pings, Config and Utils

configDef("fighter.boots", true)

function pings.fighter_boots(bool)
    model.root.LeftLeg.LeftCalf.LeftShoe:setVisible(bool)
    model.root.RightLeg.RightCalf.RightShoe:setVisible(bool)
    config:save("fighter.boots", bool)
end

pings.fighter_boots(config:load("fighter.boots"))

-- -- Action Wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Toggle red Boots")
    :item("leather_boots")
    :toggleItem("netherite_boots")
    :toggleColor(vectors.hexToRGB("dd484c"))
    :toggled(config:load("fighter.boots"))
    :onToggle(pings.fighter_boots)

return {
    name = "fighter",
    model = models.fighter,
    animations = animations.fighter,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Fighter Kit",
        item = "red_wool",
        toggleItem = "red_glazed_terracotta",
        -- color = "FFdd484c",
        toggleColor = "FFFF0000"
    }
}

