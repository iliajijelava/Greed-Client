-- Auto generated script file --

--hide vanilla model
vanilla_model.PLAYER:setVisible(false)

--hide vanilla armor model
vanilla_model.ARMOR:setVisible(false)

--hide vanilla cape model
vanilla_model.CAPE:setVisible(false)

animations.model.close:play()
models.model.root.LeftArm.group:setVisible(false)
models.model.root.RightArm.group3:setVisible(false)

function pings.test ()
    sounds:playSound("[trimmed] cyberpunk-2077-sfx-maxtac-av-siren-_sound-effect_-made-with-Voicemod",player:getPos())
end

function pings.knifes(state)
    if state then
    animations.model.open:play()
    animations.model.close:stop()
    models.model.root.LeftArm.group:setVisible(true)
    models.model.root.RightArm.group3:setVisible(true)
else
    animations.model.close:play()
    animations.model.open:stop()
end
end

local mainPage = action_wheel:newPage("main")
action_wheel:setPage(mainPage)

local action1 = mainPage:newAction()
   :setTitle ("test")
   :setItem ("minecraft:leather_chestplate")
   :onLeftClick(pings.test)

   local action1 = mainPage:newAction()
   :setTitle ("knifes")
   :setItem ("minecraft:leather_chestplate")
   :onToggle(pings.knifes)
