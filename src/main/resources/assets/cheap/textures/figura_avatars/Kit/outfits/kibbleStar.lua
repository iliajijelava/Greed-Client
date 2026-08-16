-- Action wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Sorry, no actions for this one yet :<")
    :item("red_concrete")

return {
    name = "kibbleStar",
    model = models.kibbleStar,
    animations = animations.kibbleStar,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Kibble Star Armor",
        item = "iron_helmet",
        toggleItem = "netherite_helmet",
        -- color = "FFE4D209",
        toggleColor = "ffffa209"
    }
}
