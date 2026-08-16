-- Action wheel

local page = action_wheel:newPage()

page:newAction()
    :title("Sorry, no actions for this one yet :<")
    :item("red_concrete")

return {
    name = "lawyer",
    model = models.lawyer,
    animations = animations.lawyer,
    action_wheel = page,
    transition = nil,
    action = {
        name = "Lawyer Kit",
        item = "blue_wool",
        toggleItem = "light_gray_wool",
        -- color = "FFE4D209",
        toggleColor = "FF189BCC"
    }
}
