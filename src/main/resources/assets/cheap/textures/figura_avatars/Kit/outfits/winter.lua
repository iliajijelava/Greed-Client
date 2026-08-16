-- Action wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Sorry, no actions for this one yet :<")
    :item("red_concrete")

return {
    name = "winter",
    model = models.winter,
    animations = animations.winter,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Winter Kit",
        item = "lime_wool",
        toggleItem = "green_wool",
        -- color = "FFE4D209",
        toggleColor = "FF228C19"
    }
}
