-- Action wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Sorry, no actions for this one yet :<")
    :item("red_concrete")

return {
    name = "greek",
    model = models.greek,
    animations = animations.greek,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Greek Toga Kit",
        item = "dandelion",
        toggleItem = "horn_coral",
        -- color = "FFE4D209",
        toggleColor = "FFFFFFFF"
    }
}
