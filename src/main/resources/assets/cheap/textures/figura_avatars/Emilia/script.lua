-- Auto generated script file --

--hide vanilla model
vanilla_model.PLAYER:setVisible(false)

--hide vanilla armor model
vanilla_model.ARMOR:setVisible(false)

--hide vanilla cape model
vanilla_model.CAPE:setVisible(false)



local gaze = require("Gaze")

local mainGaze = gaze:newGaze()                        -- Create a new gaze. We don't provide any arguments as the head is in-line with our camera
local Irises = models.Emilia.root.Head.Eyes.Irises
mainGaze:newEye(Irises.LeftIris, 0.25, 0.5, 0.5, 0.5) -- We will create two newEyes, one for each eye, and provide the bounds
mainGaze:newEye(Irises.RightIris, 0.5, 0.25, 0.5, 0.5)
mainGaze:newBlink(animations.Emilia.Blink)              -- We also created a blink animation, so we will put it here
