
--[[--------------------------------------------------------------------------------------
███████╗ ██████╗ ██╗   ██╗██╗███████╗██╗  ██╗██╗   ██╗     █████╗ ██████╗ ██╗
██╔════╝██╔═══██╗██║   ██║██║██╔════╝██║  ██║╚██╗ ██╔╝    ██╔══██╗██╔══██╗██║
███████╗██║   ██║██║   ██║██║███████╗███████║ ╚████╔╝     ███████║██████╔╝██║
╚════██║██║▄▄ ██║██║   ██║██║╚════██║██╔══██║  ╚██╔╝      ██╔══██║██╔═══╝ ██║
███████║╚██████╔╝╚██████╔╝██║███████║██║  ██║   ██║       ██║  ██║██║     ██║
╚══════╝ ╚══▀▀═╝  ╚═════╝ ╚═╝╚══════╝╚═╝  ╚═╝   ╚═╝       ╚═╝  ╚═╝╚═╝     ╚═╝                                                                         
--]]--------------------------------------------------------------------------------------ANSI Shadow

-- Author: Squishy
-- Discord tag: @mrsirsquishy

-- Version: 1.0.0 
-- Legal: ARR

-- Special Thanks to 
-- @jimmyhelp for errors and just generally helping me get things working.

-- IMPORTANT FOR NEW USERS!!! READ THIS!!!

-- Thank you for using SquAPI! Unless you're experienced and wish to actually modify the functionality
-- of this script, I wouldn't reccomend snooping around. 
-- Don't know exactly what you're doing? This site contains a guide on how to use!(also linked on github):
-- https://mrsirsquishy.notion.site/Squishy-API-Guide-3e72692e93a248b5bd88353c96d8e6c5

-- This SquAPI file does have some mini-documentation on paramaters if you need like a quick reference, but
-- do not modify, and do not copy-paste code from this file unless you are an avid scripter who knows what they are doing.


-- Don't be afraid to ask me for help, just make sure to provide as much info as possible so I or someone can help you faster.






--setup stuff
local squassets 
if pcall(require, "libraries.SquAssets") then
    squassets = require("libraries.SquAssets")
else
    error("§4Missing SquAssets file! Make sure to download that from the GitHub too!§c")
end
local squapi = {}


-- SQUAPI CONTROL VARIABLES AND CONFIG ----------------------------------------------------------
-------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------
-- these variables can be changed to control certain features of squapi.


--when true it will automatically tick and update all the functions, when false it won't do that. 
--if false, you can run each objects respective tick/update functions on your own - better control. 
squapi.autoFunctionUpdates = true


-- FUNCTIONS --------------------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------------------



-- TAIL PHYSICS
-- guide:(note if it has a * that means you can leave it blank to use reccomended settings)
-- tailSegmentList:		    the list of each individual tail segment of your tail
-- *idleXMovement:		    how much the tail should sway side to side
-- *idleYMovement:		    how much the tail should sway up and down
-- *idleXSpeed:			    how fast the tail should sway side to side 
-- *idleYSpeed:			    how fast the tail should sway up and down 
-- *bendStrength:		    how strongly the tail moves when you move
-- *velocityPush:		    this will cause the tail to bend when you move forward/backward, good if your tail is bent downward or upward. 
-- *initialMovementOffset:	this will offset the tails initial sway, this is good for when you have multiple tails and you want to desync them
-- *offsetBetweenSegments:	how much each tail segment should be offset from the previous one
-- *stiffness:			    how stiff the tail should be
-- *bounce:				    how bouncy the tail should be
-- *flyingOffset:		    when flying, riptiding, or swimming, it may look strange to have the tail stick out, so instead it will rotate to this value(so use this to flatten your tail during these movements)
-- *downLimit:			    the lowest each tail segment can rotate
-- *upLimit:			    the highest each tail segment can rotate

squapi.tails = {}
squapi.tail = {}
squapi.tail.__index = squapi.tail
function squapi.tail:new(tailSegmentList, idleXMovement, idleYMovement, idleXSpeed, idleYSpeed, bendStrength, velocityPush, initialMovementOffset, offsetBetweenSegments, stiffness, bounce, flyingOffset, downLimit, upLimit)
	local self = setmetatable({}, squapi.tail)

    -- INIT -------------------------------------------------------------------------
    --error checker
	if type(tailSegmentList) == "ModelPart" then
		tailSegmentList = {tailSegmentList}
	end
	assert(type(tailSegmentList) == "table", 
	"your tailSegmentList table seems to to be incorrect")
	
    self.berps = {}
    self.targets = {}
    self.stiffness = stiffness or .005
    self.bounce = bounce or .9
    self.downLimit = downLimit or -90
    self.upLimit = upLimit or 45
	for i = 1, #tailSegmentList do
		assert(tailSegmentList[i]:getType() == "GROUP",
		"§4The tail segment at position "..i.." of the table is not a group. The tail segments need to be groups that are nested inside the previous segment.§c")
        self.berps[i] = {squassets.BERP:new(self.stiffness, self.bounce), squassets.BERP:new(self.stiffness, self.bounce, self.downLimit, self.upLimit)}
        self.targets[i] = {0, 0}
    end

    self.tailSegmentList = tailSegmentList
    self.idleXMovement = idleXMovement or 15
    self.idleYMovement = idleYMovement or 5
    self.idleXSpeed = idleXSpeed or 1.2
    self.idleYSpeed = idleYSpeed or 2
    self.bendStrength = bendStrength or 2
    self.velocityPush = velocityPush or 0
    self.initialMovementOffset = initialMovementOffset or 0
    self.flyingOffset = flyingOffset or 90
    self.offsetBetweenSegments = offsetBetweenSegments or 1
    

    -- CONTROL -------------------------------------------------------------------------

    -- UPDATES -------------------------------------------------------------------------
	
	self.currentBodyRot = 0
	self.oldBodyRot = 0
	self.bodyRotSpeed = 0
	
    function self:tick()
		self.oldBodyRot = self.currentBodyRot
		self.currentBodyRot = player:getBodyYaw()
		self.bodyRotSpeed = math.max(math.min(self.currentBodyRot-self.oldBodyRot, 20), -20)

        local time = world.getTime()
		local vel = squassets.forwardVel()
		local yvel = squassets.verticalVel()
		local svel = squassets.sideVel()
		local bendStrength = self.bendStrength/(math.abs((yvel*30))+vel*30 + 1)
        local pose = player:getPose()
	
        for i = 1, #self.tailSegmentList do
            self.targets[i][1] = math.sin((time * self.idleXSpeed)/10 - (i)) * self.idleXMovement
            self.targets[i][2] = math.sin((time * self.idleYSpeed)/10 - (i * self.offsetBetweenSegments) + self.initialMovementOffset) * self.idleYMovement

            self.targets[i][1] = self.targets[i][1] + self.bodyRotSpeed*self.bendStrength + svel*self.bendStrength*40
			self.targets[i][2] = self.targets[i][2] + yvel * 15 * self.bendStrength - vel*self.bendStrength*15*self.velocityPush

			if i == 1 then
				if pose == "FALL_FLYING" or pose == "SWIMMING" or player:riptideSpinning() then
					self.targets[i][2] = self.flyingOffset
				end	
			end
			
        end

	end
	
	function self:render(dt, context)
        local pose = player:getPose()
        if pose ~= "SLEEPING" then
            for i, tail in ipairs(self.tailSegmentList) do
                tail:setOffsetRot(
                    self.berps[i][2]:berp(self.targets[i][2], dt),
                    self.berps[i][1]:berp(self.targets[i][1], dt),
                    0
                )
            end
        else
            
        end
	end


    table.insert(squapi.ears, self)
    return self
end


-- EAR PHYSICS
-- guide:(note if it has a * that means you can leave it blank to use reccomended settings)
-- leftEar:		        the left ear's model path
-- *rightEar:	        the right ear's model path, if you don't have a right ear, just leave this blank or set to nil
-- *rangeMultiplier:	how far the ears should rotate with your head, reccomended 1
-- *horizontalEars:	    if you have elf-like ears(ears that stick out horizontally), set this to true
-- *bendStrength:	    how much the ears should move when you move, reccomended 2
-- *doEarFlick:	        whether or not the ears should randomly flick, reccomended true
-- *earFlickChance:	    how often the ears should flick, reccomended 400
-- *earStiffness:	    how stiff the ears should be, reccomended 0.1
-- *earBounce:	        how bouncy the ears should be, reccomended 0.8

squapi.ears = {}
squapi.ear = {}
squapi.ear.__index = squapi.ear
function squapi.ear:new(leftEar, rightEar, rangeMultiplier, horizontalEars, bendStrength, doEarFlick, earFlickChance, earStiffness, earBounce)
	local self = setmetatable({}, squapi.ear)
    
    -- INIT -------------------------------------------------------------------------
    
    assert(leftEar,
	"§4The first ear's model path is incorrect.§c")
    self.leftEar = leftEar
    self.rightEar = rightEar
    self.horizontalEars = horizontalEars
    self.rangeMultiplier = rangeMultiplier or 1
    if self.horizontalEars then self.rangeMultiplier = self.rangeMultiplier/2 end
    self.bendStrength = bendStrength or 2
    local earStiffness = earStiffness or 0.1
    local earBounce = earBounce or 0.8
    
    if doEarFlick == nil then doEarFlick = true end
    self.doEarFlick = doEarFlick
	self.earFlickChance = earFlickChance or 400

    -- CONTROL -------------------------------------------------------------------------

    self.enabled = true
    function self:toggle()
		self.enabled = not self.enabled
	end
    function self:disable()
        self.enabled = false
    end
    function self:enable()
        self.enabled = true
    end

    -- UPDATES -------------------------------------------------------------------------

    self.eary = squassets.BERP:new(earStiffness, earBounce)
	self.earx = squassets.BERP:new(earStiffness, earBounce)
	self.earz = squassets.BERP:new(earStiffness, earBounce)
    self.targets = {0,0,0}
    self.oldpose = "STANDING"
    function self:tick()
        if self.enabled then
            local vel = math.min(math.max(-0.75, squassets.forwardVel()), 0.75)
            local yvel = math.min(math.max(-1.5, squassets.verticalVel()), 1.5)*5
            local svel = math.min(math.max(-0.5, squassets.sideVel()),0.5)
            local headrot = squassets.getHeadRot()
            local bend = self.bendStrength
            if headrot[1] < -22.5 then bend = -bend end
            
            --gives the ears a short push when crouching/uncrouching
            local pose = player:getPose()
            if pose == "CROUCHING" and self.oldpose == "STANDING" then
                self.eary.vel = self.eary.vel + 5 * self.bendStrength
            elseif pose == "STANDING" and self.oldpose == "CROUCHING" then
                self.eary.vel = self.eary.vel - 5 * self.bendStrength
            end
            self.oldpose = pose

            --main physics
            if self.horizontalEars then
                local rot = 10*bend*(yvel + vel*10) + headrot[1] * self.rangeMultiplier
                local addrot = headrot[2] * self.rangeMultiplier
                self.targets[2] = rot + addrot
                self.targets[3] = -rot + addrot
            else
                self.targets[1] = headrot[1] * self.rangeMultiplier + 2*bend*(yvel + vel * 15)
                self.targets[2] = headrot[2] * self.rangeMultiplier - svel*100*self.bendStrength
                self.targets[3] = self.targets[2]
            end

            --ear flicking
            if self.doEarFlick then
                if math.random(0, self.earFlickChance) == 1 then
                    if math.random(0, 1) == 1 then
                        self.earx.vel = self.earx.vel + 50
                    else
                        self.earz.vel = self.earz.vel - 50
                    end
                end
            end

        else
            leftEar:setOffsetRot(0,0,0)
            rightEar:setOffsetRot(0,0,0)
        end
    end

    function self:render(dt, context)
        if self.enabled then
            self.eary:berp(self.targets[1], dt)
            self.earx:berp(self.targets[2], dt)
            self.earz:berp(self.targets[3], dt)
            
            local rot3 = self.earx.pos/4
            local rot3b = self.earz.pos/4

            if self.horizontalEars then
                local y = self.eary.pos/4
                self.leftEar:setOffsetRot(y, self.earx.pos/3, rot3)
                if self.rightEar then 
                    self.rightEar:setOffsetRot(y, self.earz.pos/3, rot3b) 
                end
            else
                self.leftEar:setOffsetRot(self.eary.pos, rot3, rot3)
                if self.rightEar then 
                    self.rightEar:setOffsetRot(self.eary.pos, rot3b, rot3b) 
                end
            end
        end
    end

    table.insert(squapi.ears, self)
    return self
end


--BEWB PHYSICS
-- guide:(note if it has a * that means you can leave it blank to use reccomended settings)
-- element: 	    the bewb element that you want to affect(models.[modelname].path)
-- bendability(2):  how much the bewb should move when you move
-- stiff(0.05):	    how stiff the bewb should be
-- bounce(0.9):	    how bouncy the bewb should be
-- doIdle(true):    whether or not the bewb should have an idle sway(like breathing)
-- idleStrength(4): how much the bewb should sway when idle
-- idleSpeed(1):    how fast the bewb should sway when idle
-- downLimit(-10):  the lowest the bewb can rotate
-- upLimit(25):     the highest the bewb can rotate

squapi.bewbs = {}
squapi.bewb = {}
squapi.bewb.__index = squapi.bewb
function squapi.bewb:new(element, bendability, stiff, bounce, doIdle, idleStrength, idleSpeed, downLimit, upLimit)
    local self = setmetatable({}, squapi.bewb)

    -- INIT -------------------------------------------------------------------------
	assert(element,"§4Your model path for bewb is incorrect.§c")
    self.element = element
	if doIdle == nil then doIdle = true end
    self.doIdle = doIdle
	self.bendability = bendability or 2
	self.bewby = squassets.BERP:new(stiff or 0.05, bounce or 0.9, downLimit or -10, upLimit or 25 )
    self.idleStrength = idleStrength or 4
    self.idleSpeed = idleSpeed or 1
	self.target = 0

    -- CONTROL -------------------------------------------------------------------------

    self.enabled = true
    function self:toggle()
		self.enabled = not self.enabled
	end
    function self:disable()
        self.enabled = false
    end
    function self:enable()
        self.enabled = true
    end
    

    -- UPDATE -------------------------------------------------------------------------

    self.oldpose = "STANDING"
    function self:tick()
        if self.enabled then
            local vel = squassets.forwardVel()
            local yvel = squassets.verticalVel()
            local worldtime = world.getTime()

            if self.doIdle then 
                self.target = math.sin(worldtime/8*self.idleSpeed)*self.idleStrength
            end

            --physics when crouching/uncrouching
            local pose = player:getPose()
            if pose == "CROUCHING" and self.oldpose == "STANDING" then
                self.bewby.vel = self.bewby.vel + self.bendability
            elseif pose == "STANDING" and self.oldpose == "CROUCHING" then
                self.bewby.vel = self.bewby.vel - self.bendability
            end
            self.oldpose = pose

            --physics when moving
            self.bewby.vel = self.bewby.vel - yvel * self.bendability
            self.bewby.vel = self.bewby.vel - vel * self.bendability
        else
            self.target = 0
        end
    end

	function self:render(dt, context)
		self.element:setOffsetRot(self.bewby:berp(self.target, dt),0,0)
	end

    table.insert(squapi.bewbs, self)
    return self
end


--RANDOM ANIMATION OBJECT
--this object will take in an animation and plays it randomly every tick by a specified amount. 
--animation:    the animation to play
--*chanceRange: an optional paramater that sets the range. 0 means every tick, larger values mean lower chances of playing every tick.
--*isBlink:     if this is for blinking set this to true so that it doesn't blink while sleeping. 

-- squapi.randimation = {}
-- squapi.randimation.__index = squapi.randimation
-- function squapi.randimation:new(animation, chanceRange, isBlink)
-- 	local self = setmetatable({}, squapi.randimation)
	
--     -- INIT -------------------------------------------------------------------------
--     self.isBlink = isBlink
--     self.animation = animation
-- 	self.chanceRange = chanceRange or 200


--     -- CONTROL -------------------------------------------------------------------------
	
--     self.enabled = true
--     function self:toggle()
-- 		self.enabled = not self.enabled
-- 	end
--     function self:disable()
--         self.enabled = false
--     end
--     function self:enable()
--         self.enabled = true
--     end

--     -- UPDATES -------------------------------------------------------------------------

-- 	function events.tick()
-- 		if self.enabled and (not self.isBlink or player:getPose() ~= "SLEEPING") and math.random(0, self.chanceRange) == 0 and self.animation:isStopped() then
--             self.animation:play()
-- 		end
-- 	end

-- 	return self
-- end


-- -- MOVING EYES
-- -- guide:(note if it has a * that means you can leave it blank to use reccomended settings)
-- -- element:	 		the eye element that is going to be moved, each eye is seperate.
-- -- *leftdistance: 	the distance from the eye to it's leftmost posistion
-- -- *rightdistance: 	the distance from the eye to it's rightmost posistion
-- -- *updistance: 	the distance from the eye to it's upmost posistion
-- -- *downdistance: 	the distance from the eye to it's downmost posistion
-- squapi.eyes = {}
-- squapi.eye = {}
-- squapi.eye.__index = squapi.eye
-- function squapi.eye:new(element, leftDistance, rightDistance, upDistance, downDistance, switchValues)
--     local self = setmetatable({}, squapi.eye)

--     -- INIT -------------------------------------------------------------------------
-- 	assert(element,
-- 	"§4Your eye model path is incorrect.§c")
-- 	self.switchValues = switchValues or false
-- 	self.left = leftDistance or .25
-- 	self.right = rightDistance or 1.25
-- 	self.up = upDistance or 0.5
-- 	self.down = downDistance or 0.5
	
--     self.x = 0 
--     self.y = 0
--     self.eyeScale = 1

--     -- CONTROL -------------------------------------------------------------------------

--     --For funzies if you want to change the scale of the eyes you can use this.(lerps to scale)
--     function self:setEyeScale(scale)
--         self.eyeScale = scale 
--     end

--     self.enabled = true
--     function self:toggle()
-- 		self.enabled = not self.enabled
-- 	end
--     function self:disable()
--         self.enabled = false
--     end
--     function self:enable()
--         self.enabled = true
--     end

--     --resets position
--     function self:zero()
--         self.x, self.y = 0, 0
--     end

--     -- UPDATES -------------------------------------------------------------------------

--     function self:tick()
--         if self.enabled then 
--             local headrot = squassets.getHeadRot()
--             headrot[2] = math.max(math.min(50, headrot[2]), -50)

--             --parabolic curve so that you can control the middle position of the eyes. 
--             self.x = -squassets.parabolagraph(-50, -self.left, 0,0, 50, self.right, headrot[2])
--             self.y = squassets.parabolagraph(-90, -self.down, 0,0, 90, self.up, headrot[1])
            
--             --prevents any eye shenanigans
--             self.x = math.max(math.min(self.left, self.x), -self.right)
--             self.y = math.max(math.min(self.up, self.y), -self.down)
--         end

--     end

-- 	function self:render(dt, context)
--         local c = element:getPos()
-- 		if self.switchValues then
-- 			element:setPos(0,math.lerp(c[2], self.y, dt),math.lerp(c[3], -self.x, dt))
-- 		else
-- 			element:setPos(math.lerp(c[1], self.x, dt),math.lerp(c[2], self.y, dt),0)
-- 		end
--         local scale = math.lerp(element:getOffsetScale()[1], self.eyeScale, dt)
-- 		element:setOffsetScale(scale, scale, scale)
-- 	end

--     table.insert(squapi.eyes, self)
--     return self
-- end	


-- -- SMOOTH HEAD - Mimics a vanilla player head, but smoother and with some extra life. can also do smooth Torsos and Smooth Necks!
-- -- guide:(note if it has a * that means you can leave it blank to use reccomended settings)
-- -- element: 			 The head element that you wish to effect
-- -- *strength:            The target rotation is multiplied by this factor. For example setting to 1 will follow vanilla rotation, 0.5 is half of that, and 2 is double vanilla rotation. 
-- -- *tilt:                For context the smooth head applies a slight tilt to the head as it's rotated toward the side, this controls the strength of that tilt.  
-- -- *speed:               How fast the head will rotate toward the target rotation. For example 1 is base speed, 0.5 is half of that, and 2 is double speed. 
-- -- *keepOriginalHeadPos: When true(automatically true) the heads position will follow the vanilla head position. For example when crouching the head will shift down to follow. set to false to disable.

-- --Smooth Neck? Smooth Torso?
-- --This can do that too if you change what you input for these:
-- -- element:     Instead of a single element, input a table of head elements(imagine it like {element1, element2, etc.}). This will apply the head rotations to each of these.
-- -- *strength:   Instead of an single number, you can put in a table(imagine it like {strength1, strength2, etc.}). This will apply each strength to each respective element.(make sure it is the same length as your element table)
-- -- As a tip, you can imagine the strength as a percentage of the heads vanilla rotation. 
-- -- So if you have a head and a torso, you might do 0.5 for the head, and 0.5 for the torso to add up to 1(100% of the vanilla heads rotation), or maybe even 0.25 for torso, and 0.75 for head, it's up to you!

-- squapi.smoothHeads = {}
-- squapi.smoothHead = {}
-- squapi.smoothHead.__index = squapi.smoothHead
-- function squapi.smoothHead:new(element, strength, tilt, speed, keepOriginalHeadPos)
--     local self = setmetatable({}, squapi.smoothHead)
	
--     -- INIT -------------------------------------------------------------------------
--     if type(element) == "ModelPart" then
--         assert(element, "§4Your model path for smoothHead is incorrect.§c") 
-- 		element = {element}
-- 	end
--     assert(type(element) == "table", "§4your element table seems to to be incorrect.§c")
    
--     for i = 1, #element do
--         assert(element[i]:getType() == "GROUP",
-- 		"§4The head element at position "..i.." of the table is not a group. The head elements need to be groups that are nested inside one another to function properly.§c")
-- 		assert(element[i], "§4The head segment at position "..i.." is incorrect.§c")
--         element[i]:setParentType("NONE")
--     end
--     self.element = element

--     self.strength = strength or 1 
--     if type(self.strength) == "number" then
--         local strengthDiv = self.strength/#element
--         self.strength = {}
-- 		for i = 1, #element do
--             self.strength[i] = strengthDiv
--         end
-- 	end

-- 	self.tilt = tilt or 0.1
-- 	if keepOriginalHeadPos == nil then keepOriginalHeadPos = true end
--     self.keepOriginalHeadPos = keepOriginalHeadPos
--     self.headRot = vec(0, 0, 0) 
-- 	self.offset = vec(0, 0, 0)
--     self.speed = (speed or 1)/2

--     -- CONTROL -------------------------------------------------------------------------


--     -- Applies an offset to the heads rotation to more easily modify it. Applies as a vector.(for multisegments it will modify the target rotation)
--     function self:setOffset(xRot, yRot, zRot)
--         self.offset = vec(xRot, yRot, zRot)
--     end

--     self.enabled = true
--     function self:toggle()
-- 		self.enabled = not self.enabled
-- 	end
--     function self:disable()
--         self.enabled = false
--     end
--     function self:enable()
--         self.enabled = true
--     end

--     function self:zero()
--         for i, v in pairs(self.element) do
--             v:setPos(0, 0, 0)
--             v:setOffsetRot(0, 0, 0)
--             self.headRot = vec(0,0,0)
--         end
--     end

    
    
--     -- UPDATE -------------------------------------------------------------------------
--     function self:tick()
--         if self.enabled then
--             local vanillaHeadRot = squassets.getHeadRot()
            
--             self.headRot[1] = self.headRot[1] + (vanillaHeadRot[1] - self.headRot[1])*self.speed
--             self.headRot[2] = self.headRot[2] + (vanillaHeadRot[2] - self.headRot[2])*self.speed
--             self.headRot[3] = self.headRot[2]*self.tilt
--         end
--     end

-- 	function self:render(dt, context) 
--         if self.enabled then
--             dt = dt/5
--             for i, v in pairs(self.element) do
--                 local c = self.element[i]:getOffsetRot()
--                 local target = (self.headRot*self.strength[i])-self.offset/#self.element
--                 self.element[i]:setOffsetRot(math.lerp(c[1], target[1], dt), math.lerp(c[2], target[2], dt), math.lerp(c[3], target[3], dt))

--                 -- Better Combat SquAPI Compatibility created by @jimmyhelp and @foxy2526 on Discord
--                 if renderer:isFirstPerson() and context == "RENDER" then
--                     self.element[i]:setVisible(false)
--                 else
--                     self.element[i]:setVisible(true)
--                 end
--             end
            
--             if self.keepOriginalHeadPos then 
--                 self.element[#self.element]:setPos(-vanilla_model.HEAD:getOriginPos()) 
--             end
--         end
-- 	end

--     table.insert(squapi.smoothHeads, self)
--     return self
-- end


-- UPDATES ALL SQUAPI FEATURES --------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------------------
---------------------------------------------------------------------------------------------------------------------------

if squapi.autoFunctionUpdates then

    function events.tick()
        -- for i, v in pairs(squapi.smoothHeads) do
        --     v:tick()
        -- end
        -- for i, v in pairs(squapi.eyes) do
        --     v:tick()
        -- end
        for i, v in pairs(squapi.bewbs) do
            v:tick()
        end
        for i, v in pairs(squapi.ears) do
            v:tick()
        end
        for i, v in pairs(squapi.tails) do
            v:tick()
        end
    end

    function events.render(dt, context)
        -- for i, v in pairs(squapi.smoothHeads) do
        --     v:render(dt, context)
        -- end
        -- for i, v in pairs(squapi.eyes) do
        --     v:render(dt, context)
        -- end
        for i, v in pairs(squapi.bewbs) do
            v:render(dt, context)
        end
        for i, v in pairs(squapi.ears) do
            v:render(dt, context)
        end
        for i, v in pairs(squapi.tails) do
            v:render(dt, context)
        end
    end

end



return squapi