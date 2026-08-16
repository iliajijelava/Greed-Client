-- Auto generated script file --

--hide vanilla stuff
vanilla_model.PLAYER:setVisible(false)
vanilla_model.ELYTRA:setVisible(false)
vanilla_model.ARMOR:setVisible(false)
vanilla_model.CAPE:setVisible(false)

require("GSAnimBlend")
local smoothie = require("Smoothie")
local anim = animations.rofdragon
local model = models.rofdragon

local membrane = require("membrane")


--model:setScale(0.5, 0.5, 0.5)
--hide icarus wings
if icarus then
    icarus:setWingsVisible(false)
end

---Animation Speeds
	anim.flap:setSpeed(0.95)
	anim.glide:setSpeed(0.5)
	anim.glideup:setSpeed(0.5)
	anim.left:setSpeed(0.5)
	anim.right:setSpeed(0.5)
	anim.airstall:setSpeed(0.5)
	anim.walk:setSpeed(0.6)
	anim.sprint:setSpeed(0.8)
	anim.idle:setSpeed(0.2)
	anim.crouch:setSpeed(0.2)
	anim.crouchwalk:setSpeed(0.3)
	anim.flywalk:setSpeed(0.7)
	anim.swim:setSpeed(0.3)
	anim.waterwalk:setSpeed(0.2)
	anim.fly:setSpeed(0.7)

--eyeglow

function events.tick()
	if world.getLightLevel(player:getPos()) < 6 then
		model.root.body.chest.neck1.neck2.neck3.head.eyes:setPrimaryRenderType("EMISSIVE")
		else model.root.body.chest.neck1.neck2.neck3.head.eyes:setPrimaryRenderType("TRANSLUCENT")
	end
end

local BlinkTime = 150

function events.tick()
	if BlinkTime > 1 then BlinkTime=BlinkTime-1 end
	if BlinkTime == 1 then BlinkTime = 150 end
	if BlinkTime < 15 then model.root.body.chest.neck1.neck2.neck3.head.eyelids:setVisible(true) else model.root.body.chest.neck1.neck2.neck3.head.eyelids:setVisible(false) end
end
--MEMBRANE


local membrane = require("membrane")

--- Membrane: Panel 1
membrane:define(models.rofdragon.Membranes.Panel_1, {
    model.root.body.chest.wings.leftwingshoulder.leftwing1.LeftMem1ConnectA2,
    model.root.body.chest.LeftMem1ConnectA1,
    model.root.body.waist.hips.LeftMem1ConnectA4,
    model.root.body.chest.wings.leftwingshoulder.leftwing1.leftelbowrod.LeftMem1ConnectA3,
})

--- Membrane: Panel 1_copy
membrane:define(models.rofdragon.Membranes.Panel_1_copy, {
    model.root.body.chest.wings.rightwingshoulder.rightwing1.RightMem1ConnectA2,
    model.root.body.chest.RightMem1ConnectA1,
    model.root.body.waist.hips.RightMem1ConnectA4,
    model.root.body.chest.wings.rightwingshoulder.rightwing1.rightelbowrod.RightMem1ConnectA3,
})

--- Membrane: Panel 2
membrane:define(models.rofdragon.Membranes.Panel_2, {
	model.root.body.chest.wings.leftwingshoulder.leftwing1.leftelbowrod.LeftMem1ConnectB2,
	model.root.body.waist.hips.LeftMem1ConnectB3,
	model.root.body.waist.hips.tail1.LeftMem1ConnectB4,
	model.root.body.chest.wings.leftwingshoulder.leftwing1.leftelbowrod.leftelbowrod2.LeftMem1ConnectB1
})

--- Membrane: Panel 2_copy
membrane:define(models.rofdragon.Membranes.Panel_2_copy, {
	model.root.body.chest.wings.rightwingshoulder.rightwing1.rightelbowrod.RightMem1ConnectB2,
	model.root.body.waist.hips.RightMem1ConnectB3,
	model.root.body.waist.hips.tail1.RightMem1ConnectB4,
	model.root.body.chest.wings.rightwingshoulder.rightwing1.rightelbowrod.rightelbowrod2.RightMem1ConnectB1
})

--- Membrane: Panel 3
membrane:define(models.rofdragon.Membranes.Panel_3, {
	model.root.body.waist.hips.tail1.tailfinleft.LeftMem1ConnectC4,
	model.root.body.chest.wings.leftwingshoulder.leftwing1.leftelbowrod.leftelbowrod2.LeftMem1ConnectC1,
	model.root.body.waist.hips.tail1.LeftMem1ConnectC2,
	model.root.body.waist.hips.tail1.LeftMem1ConnectC3
})
--- Membrane: Panel 3_copy
membrane:define(models.rofdragon.Membranes.Panel_3_copy, {
	model.root.body.chest.wings.rightwingshoulder.rightwing1.rightelbowrod.rightelbowrod2.RightMem1ConnectC1,
	model.root.body.waist.hips.tail1.RightMem1ConnectC2,
	model.root.body.waist.hips.tail1.RightMem1ConnectC3,
	model.root.body.waist.hips.tail1.tailfinright.RightMem1ConnectC4
})

--- Membrane: Panel 7
membrane:define(models.rofdragon.Membranes.Panel_7, {
    models.rofdragon.root.body.waist.hips.tail1.tail2.tail2fanLeft.LeftMem1ConnectD4,
    models.rofdragon.root.body.waist.hips.tail1.tailfinleft.LeftMem1ConnectD2,
    models.rofdragon.root.body.waist.hips.tail1.tailfinleft.LeftMem1ConnectD1,
    models.rofdragon.root.body.waist.hips.tail1.tail2.tail2fanLeft.LeftMem1ConnectD3,
})

--- Membrane: Panel 7_copy
membrane:define(models.rofdragon.Membranes.Panel_7_copy, {
    models.rofdragon.root.body.waist.hips.tail1.tail2.tail2fanRight.RightMem1ConnectD4,
    models.rofdragon.root.body.waist.hips.tail1.tailfinright.RightMem1ConnectD2,
    models.rofdragon.root.body.waist.hips.tail1.tailfinright.RightMem1ConnectD1,
    models.rofdragon.root.body.waist.hips.tail1.tail2.tail2fanRight.RightMem1ConnectD3,
})



	--tail--
local tailPhysics = require('tail')

local tail = tailPhysics.new(model.root.body.waist.hips.tail1)

tail:setConfig {
   idleSpeed = vec(0.025, 0.05, 0), 
   idleStrength = vec(3, 3, 0), 
   rotVelocityStrength = 1,
}

local smoothHead1 = smoothie:newSmoothHead(model.root.body.chest.neck1)
local smoothHead2 = smoothie:newSmoothHead(model.root.body.chest.neck1.neck2)
local smoothHead3 = smoothie:newSmoothHead(model.root.body.chest.neck1.neck2.neck3)
local smoothHead4 = smoothie:newSmoothHead(model.root.body.chest.neck1.neck2.neck3.head)

		smoothHead4:strength(0.4)
		smoothHead3:strength(0.4)
		smoothHead2:strength(0.5)
		smoothHead1:strength(0.4)
		smoothHead2:tiltMultiplier(1)
		smoothHead1:tiltMultiplier(1)
		smoothHead2:speed(1)
		smoothHead1:speed(1)
		
		
--camera stuff
function events.tick()
	if not player:isGliding() and renderer:isFirstPerson() then
		renderer:setCameraPos(0, 0, 0)
	end
end


--pings--
function pings.offset()
	renderer:setCameraPos(3, 4, 11)
end

function pings.flightcam()
	renderer:setCameraPos(0, 5, 9)
end

function pings.default()
	renderer:setCameraPos(0, 0, 0)
end

--action wheel

local mainPage = action_wheel:newPage()
action_wheel:setPage(mainPage)
local camPage = action_wheel:newPage()

local toSecond = mainPage:newAction()
    :title("Next Page")
    :item("white_stained_glass_pane")
    :onLeftClick(function()
    action_wheel:setPage(camPage)
end)
   
	
local toMain = camPage:newAction()
    :title("Previous Page")
    :item("white_stained_glass_pane")
    :onLeftClick(function()
    action_wheel:setPage(mainPage)
    end)

local action = camPage:newAction()
    :title("CAMERA: Pulled Back Offset")
    :item("minecraft:ender_pearl")
    :hoverColor(1, 0, 1)
    :onLeftClick(pings.offset)
	
local action = camPage:newAction()
    :title("CAMERA: Pulled Back")
    :item("minecraft:ender_eye")
    :hoverColor(1, 0, 1)
    :onLeftClick(pings.flightcam)
	
local action = camPage:newAction()
    :title("CAMERA: Default")
    :item("minecraft:diamond")
    :hoverColor(1, 0, 1)
    :onLeftClick(pings.default)
	