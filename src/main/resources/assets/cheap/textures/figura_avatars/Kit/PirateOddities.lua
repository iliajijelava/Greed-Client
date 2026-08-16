--[[
PIRATE ODDITIES v0.11

newest:
	only left ear can be specified for ears object

a collection of helpful, but perhaps odd, functions

helper functions
Random
RandomVec3
HeadOriginRot
SquareSign
sinWave
getLocalVelocity

helper variables
justJumped
startCrouching

object list
BounceValue
Tail
Ears
Eyes
BouncePart
Wings
SmoothRot
Heartbeat
BounceWalk
]]--

PirOdd = {}
PirOdd.registered = {}

-- outputs a random float between -range to range
---@param range number, maximum/minimum value
---@retrun number
function PirOdd.Random(range)
	return (math.random()-0.5)*range*2
end

-- outputs a vec3 containing 3 random floats between -range to range
---@param range number, maximum/minimum value
---@return Vector3
function PirOdd.RandomVec3(range)
	return vec(PirOdd.Random(range),PirOdd.Random(range),PirOdd.Random(range))
end

-- returns head origin rot that wraps bwetween -180 and 180
-- identical to similarly named function in squapi
---@return Vector3
function PirOdd.HeadOriginRot()
	return (vanilla_model.HEAD:getOriginRot() + 180) % 360 - 180
end

-- squares the number (or a vector3) and applies the sign it had before squaring
function PirOdd.SquareSign(value)
	if type(value) == "Vector3" then
		for i=1,3 do
			value[i] = PirOdd.SquareSign(value[i])
		end
		return value
	else
		return value^2*math.sign(value)
	end
end

-- returns a value on a sine wave
---@param x number, x-value on graph
---@param period number, period of graph (units before repeating)
---@param amplitude number, amplitude of graph (height from baseline to maximum)
---@param baseline number, baseline of graph (middle value)
---@return number
function PirOdd.sinWave(x, period, amplitude, baseline)
	
	return math.sin((x*math.pi*2)/(period or 1))*(amplitude or 1)+(baseline or 0)
end

-- stolen from GSExtensions
function getLocalVelocity()
	return matrices.mat4()
		:reset()
		:rotateY(player:getRot(client:getFrameTime()).y)
		:scale(vec(-1, 1, -1))
		:applyDir(player:getVelocity())
end

-- BounceValue metatable
local BounceBase = {
	-- sets the bounce target
	---@param self PirOdd.PirOddBounceValue, the bounce object
	---@param target Vector3, the target to bounce towards
	setTarget = function(self,target)
			self.target = target
			return self
		end,
	-- resets all velocity and position
	---@param self PirOdd.PirOddBounceValue, the bounce object
	reset = function(self)
			self.velocity = vec(0,0,0)
			self.acceleration = vec(0,0,0)
			self.pos = vec(0,0,0)
		end,
	-- to be called every tick - updates physics
	---@param self PirOdd.PirOddBounceValue, the bounce object
	---@param forceApplied Vector3, applies a force to the object
	---@param target Vector3, temporarly sets the target this tick
	updateTick = function(self, forceApplied, target)
			self.oldPos = self.pos
			
			local actualTarget= target or self.target
			local diff = self.pos-actualTarget
			
			-- forces
			-- spring force F = -kx
			local springForce = -self.stiffness*diff
			-- made up bounce force
			-- grants velocity based on velocity
			local bounceForce = 0--diff:normalize()*self.bounciness*self.velocity:length()
			
			-- friction
			self.velocity = self.velocity*(1-self.drag)
			
			-- apply
			-- a = F/m
			self.acceleration = (springForce+bounceForce+(forceApplied or 0))/self.mass
			self.velocity = self.velocity+self.acceleration
			
			self.pos = self.pos+self.velocity
			
			-- clamping
			for i=1,3 do
				if self.max and self.pos[i] > self.max[i] then
					self.pos[i] = self.max[i]
					self.velocity[i] = -self.velocity[i]*self.bounciness
				end
				if self.min and self.pos[i] < self.min[i] then
					self.pos[i] = self.min[i]
					self.velocity[i] = -self.velocity[i]*self.bounciness
				end
			end
			return self
		end,
	-- to be called every render frame - lerps to the bounce object's pos
	---@param self PirOdd.PirOddBounceValue, the bounce object
	---@param _ nil, unused
	---@param delta number, time between ticks
	updateRender = function(self,_,delta)
			return math.lerp(self.oldPos,self.pos,delta)
		end,

	__type = "PirOddBounceValue"
}

BounceBase.__index = BounceBase

---@param
---@param stiffness number, strength of return force
function PirOdd.BounceValue(bounciness, stiffness, min, max, mass, drag)
	hnd = setmetatable({
		bounciness = bounciness or 0.1,
		stiffness = stiffness or 0.05,
		drag = drag or 0.1,
		max = max,
		min = min,
		mass = mass or 1,
		
		target = vec(0,0,0),
		velocity = vec(0,0,0),
		acceleration = vec(0,0,0),
		pos = vec(0,0,0),
		oldPos = vec(0,0,0)
	},BounceBase)
	
	return hnd
end

-- eyes metatable
local EyesBase = {
	ForceClose = function(self,bool)
			self.forceClose = bool
		end,
	ForceBlink = function(self)
			self.blink:play()
			self.blinkRandomAnimation.lastPlay = 0
		end,
	tick = function(self)
		if player:isLoaded() then
			-- blink			
			local blinkEnable = player:getPose() ~= "SLEEPING" and not self.forceClose
			if self.blinkRandomAnimation.enabled ~= blinkEnable then
				self.blinkRandomAnimation:setEnabled(blinkEnable)
			end

			-- close eyes
			if self.closeEye then
				self.closeEye:setPlaying(player:getPose() == "SLEEPING" or self.forceClose)
			end
		end
	end,
	render = function(self,delta)
		if player:isLoaded() then
			-- eye movement
			local headrot = PirOdd.HeadOriginRot()

			local multiX = math.map(math.clamp(headrot.y,-50,50), -50,50,1,-1)
			local multiY = math.map(math.clamp(headrot.x,-90,90), -90,90,-1,1)
			
			multiX = multiX^2*math.sign(multiX)
			multiY = multiY^2*math.sign(multiY)
			
			local leftPos = vec(0,0,0)
			local rightPos = vec(0,0,0)
			
			if multiY > 0 then
				leftPos.y = multiY*self.up
				rightPos.y = multiY*self.up
			else
				leftPos.y = multiY*self.down
				rightPos.y = multiY*self.down
			end
			
			if multiX > 0 then
				leftPos.x = multiX*self.left
				rightPos.x = multiX*self.right
			else
				leftPos.x = multiX*self.right
				rightPos.x = multiX*self.left
			end
			
			self.element:setPos(leftPos)
			self.element2:setPos(rightPos)
		end
	end,
	__type = "PirOddEyes"
}

EyesBase.__index = EyesBase

PirOdd.registered.eyes = {}

-- handles all various eye-related animations
---@param element ModelPart, left eye
---@param element2 ModelPart, right eye
---@param blink Animation?, blink animation
---@param closeEye Animation?, closed eye animation, for sleeping
---@param left number?, maximum left eye can move to the left (mirrored for right eye)
---@param left number?, maximum left eye can move to the right (mirrored for right eye)
---@param up number?, maximum eye movement upwards
---@param down number?, maximum eye movement downwards
---@return PirOdd.PirOddEyes
function PirOdd.Eyes(element, element2, blink, closeEye, left, right, up, down)
	local hnd = setmetatable({
		element = element,
		element2 = element2,
		
		closeEye = closeEye,
		left = left or 0.25,
		right = right or 1.25,
		up = up or 0.5,
		down = down or 0.5,
		
		blinkRandomAnimation = PirOdd.RandomAnimation(blink),
		
		forceClose = false,
		
		enabled = true,
	},EyesBase)

	
	table.insert(PirOdd.registered.eyes, hnd)
	return hnd
end

local baseWing = {
	setMass = function(self,value)
			self.mass = value
			for i,v in ipairs(self.linkedWings) do
				bounceValues[i].mass = mass
			end
		end,
	tick = function(self)
			if player:isLoaded() then	
				local velocityCoefficent = 3
				
				local vertVel
				local fwdVel
				
				if player:getPose() == "FALL_FLYING" or player:getPose() == "CRAWLING" or player:getPose() == "SWIMMING" or player:riptideSpinning() then
					-- swap axis, since in these poses the model is rotated so "up" on the model is forward in world space
					vertVel = PirOdd.localVel.z
					fwdVel = PirOdd.localVel.y
				else
					vertVel = PirOdd.localVel.y
					fwdVel = PirOdd.localVel.z
				end
			
				-- velocity mirrored between wings
				local velocity = vec(0,-fwdVel,vertVel/2)*velocityCoefficent
				
				-- low health quiver
				if player:getHealth() < 10 then
					-- parabola - vertex 0,0.5 - roots 10, -10
					-- makes a smooth curve up as health decreases to 0 up to a max value of 0.05
					local healthCoefficent = -0.005*(player:getHealth()-10)*(player:getHealth()+10)
					
					velocity = velocity+PirOdd.RandomVec3(healthCoefficent)
				end
				
				if player:getNbt().HurtTime > 5 then
					
					velocity = velocity+PirOdd.RandomVec3(2)
				end
				
				-- delta kept same between wings
				local rotOffset = vec(math.min(5,vertVel*velocityCoefficent/2),math.clamp(PirOdd.bodyRotDelta, -10, 10),0)

				-- yaw offsetting			
				self.oldYawOffset = self.yawOffset
				self.yawOffset = self.yawOffset+PirOdd.bodyRotDelta
			
				-- coming back with vel
				if fwdVel ~= 0 then
					if self.yawOffset > 0 then
						self.yawOffset = self.yawOffset+fwdVel*4
					
						if self.yawOffset < 0 then
							self.yawOffset = 0
						end
					elseif self.yawOffset < 0 then
						self.yawOffset = self.yawOffset-fwdVel*4
						
						if self.yawOffset > 0 then
							self.yawOffset = 0
						end
					end
				end
				
				-- clamp
				self.yawOffset = math.clamp(self.yawOffset,-15,15)
				
				for i,wing in ipairs(self.linkedWings) do
					local target = rotOffset * (math.abs(wing.multi) or 1)+vec(0,self.yawOffset*0.1,0)
					self.bounceValues[i]:setTarget(target):updateTick(velocity * (wing.multi or 1))
				end
			end
		end,
	render = function(self, delta)
			for i,wing in ipairs(self.linkedWings) do
				self.currentYawOffset = math.lerp(self.currentYawOffset, self.yawOffset, delta)
			
				local value = self.bounceValues[i]:updateRender(wing.part:getOffsetRot()-vec(0,self.currentYawOffset,0), delta)
			
				wing.part:setOffsetRot(value+vec(0,self.currentYawOffset,0))
				if wing.propagate then
					for i,v in ipairs(wing.propagate) do
						v:setOffsetRot(value*1/i)
					end
				end
			end
		end,
		
	__type = "PirOddWing"
}

PirOdd.registered.wings = {}

baseWing.__index = baseWing

-- wing physics
---@param linked_wings table, table of connected wings
---@param mass number?, reduces acceleration
---@return PirOdd.PirOddWing
function PirOdd.Wings(linked_wings, mass)
	local hnd = setmetatable({
		mass = mass or 1,
		linkedWings = linked_wings,
		wingRoot = wing_root,
		
		oldBodyRot = 0,
		
		yawOffset = 0,
		currentYawOffset = 0,
		
		bounceValues = {},
		
		enabled = true,
	}, baseWing)
	
	for i,v in ipairs(linked_wings) do
		hnd.bounceValues[i] = PirOdd.BounceValue(nil,nil,vec(-35,-20,-15),vec(35,20,15), hnd.mass)
	end
	
	table.insert(PirOdd.registered.wings, hnd)
	return hnd
end

local smoothRotBase = {
	enable = function(self)
			self.enabled = true
			
			self.bounce:reset()
		end,
	disable = function(self)
			self.enabled = false
			-- reset
			self.currentRot = vec(0,0,0)
			self.headPath:setOffsetRot(0,0,0)
			self.bodyRoot:setOffsetRot(0,0,0)
			for k,part in pairs(self.armPaths) do
				part:setOffsetRot(0,0,0)
			end
		end,
	setEnabled = function(self, state)
			if state then
				self:enable()
			else
				self:disable()
			end
		end,
	
	tick = function(self)
			if self.enabled then
				local targetRot = self.forceLook or PirOdd.HeadOriginRot()
				self.bounce:setTarget(targetRot):updateTick()
			end
		end,
	render = function(self, delta)
			if self.enabled then
				self.currentRot = self.bounce:updateRender(self.currentRot,delta)
			
				local tilt = self.currentRot.y*self.tiltFactor
				
				local velMod = vec(math.min(PirOdd.localVel.z+PirOdd.localVel.y,1),0,math.min(PirOdd.localVel.x,0.5))*self.velIntensity
				local breatheFactor = vec(PirOdd.sinWave(world:getTime(delta),200,self.breatheIntensity,self.breatheIntensity),0,0)

				-- rotation of head
				local headRot = (self.currentRot+vec(0,0,tilt*-0.5))*(1-self.bodyFactor)-velMod
				headRot = headRot+breatheFactor

				-- apply and apply pos again
				self.headPath:setOffsetRot(headRot)
				
				self.bodyRoot:setPos(player:isCrouching() and self.crouchBody or vec(0,0,0))
				self.headPath:setPos(player:isCrouching() and self.crouchHead or vec(0,0,0))
				
				local bodyRot = ((self.currentRot+vec(0,0,tilt))*self.bodyFactor)-breatheFactor+velMod
				
				self.bodyRoot:setOffsetRot(bodyRot)
				
				for k,part in pairs(self.armPaths) do
					part:setOffsetRot(-bodyRot*self.armFactor)
				end
				
				self.instantHeadRot = headRot
				self.instantBodyRot = bodyRot
			else
				self.headPath:setRot(vanilla_model.head:getOriginRot()):setPos(vanilla_model.head:getOriginPos())
			end
		end,
	__type = "PirOddSmoothRot"
}

smoothRotBase.__index = smoothRotBase

PirOdd.registered.smoothRots = {}

-- smoothly look around
---@param bodyRoot Group, root part for whole upper body, should contain body, arms, and legs
function PirOdd.SmoothRot(bodyRoot)
	local hnd = setmetatable({
		bodyRoot = bodyRoot,
		headPath = bodyRoot.Head,
		
		armPaths = {bodyRoot.LeftArm, bodyRoot.RightArm},
		
		bodyFactor = 0.3,
		armFactor = 0.5,
		tiltFactor = 0.25,
		intensity = 1,
		
		breatheIntensity = 3,
		velIntensity = 20,
		
		currentRot = vec(0,0,0),
		instantHeadRot = vec(0,0,0),
		instantBodyRot = vec(0,0,0),
		
		crouchBody = vec(0,0,0),
		crouchHead = vec(0,0,0),
		
		bounce = PirOdd.BounceValue(),
		
		forceLook = false,
		
		enabled = true
	},smoothRotBase)
	
	-- un-parent
	hnd.headPath:setParentType("MODEL")
	
	hnd.bounce.stiffness = 0.25
	hnd.bounce.drag = 0.4
	hnd.bounce.mass = 2
	
	table.insert(PirOdd.registered.smoothRots, hnd)
	return hnd
end

local tailBase = {
	setEnabled = function(self, bool)
			self.enabled = bool
			for i,part in ipairs(self.paths) do
				part:setOffsetRot(0,0,0)
			end
		end,
	tick = function(self)
			local velForce = vec(PirOdd.localVel.y+PirOdd.localVel.z*0.5,0,-PirOdd.localVel.x)*self.velIntensity
			
			velForce.y = velForce.y + PirOdd.bodyRotDelta*-0.1
			
			local idleOffset = vec(0,0,0)
			
			if player:getPose() == "FALL_FLYING" or player:getPose() == "CRAWLING" or player:getPose() == "SWIMMING" or player:riptideSpinning() then
				idleOffset.x = idleOffset.x+30
			end
			
			for i,v in ipairs(self.bounces) do
				local segOffset = self.offset+self.segmentOffset*i
				local idle = vec(PirOdd.sinWave(world:getTime(segOffset),self.idleYPeriod,self.idleYIntensity,0),
					PirOdd.sinWave(world:getTime(segOffset),self.idleXPeriod,self.idleXIntensity,0),0)
				v:setTarget(idle/i/self.segmentDecay+idleOffset):updateTick(velForce/i/self.segmentDecay)
			end
		end,
	render = function(self,delta)
			for i,part in ipairs(self.paths) do
				part:setOffsetRot(self.bounces[i]:updateRender(part:getOffsetRot(),delta))
			end
		end,
	__type = "PirOddTail"
}

tailBase.__index = tailBase

PirOdd.registered.tails = {}

function PirOdd.tail(tailRoot, xlimit, ylimit)
	local hnd = setmetatable({
		tailRoot = tailRoot,
		
		idleXPeriod = 60,
		idleXIntensity = 15,
		
		idleYPeriod = 40,
		idleYIntensity = 5,
		
		velIntensity = 4,
		
		offset = 0,
		segmentOffset = 4,
		segmentDecay = 1.8,
		
		paths = {},
		bounces = {},
		
		enabled = true
	}, tailBase)
	
	xlimit = xlimit or 45
	ylimit = ylimit or 45
	
	if type(tailRoot) == "table" then
		for i,v in ipairs(tailRoot) do
			hnd.paths[i] = v
			hnd.bounces[i] = PirOdd.BounceValue(0.1, 0.25, vec(-xlimit,-ylimit,-2), vec(xlimit,ylimit,2), 1.5, 0.2)
		end
		hnd.tailRoot = tailRoot[1]
	elseif type(tailRoot) == "ModelPart" then
		local name = tailRoot:getName()
		local index = (tonumber(name:sub(name:find("%d+") or 0, -1)) or 1)+1
		name = name:gsub("%d+", "")

		local currentTail = hnd.tailRoot
		hnd.paths[1] = hnd.tailRoot
		hnd.bounces[1] = PirOdd.BounceValue(0.1, 0.25, vec(-xlimit,-ylimit,-2), vec(xlimit,ylimit,2), 1.5, 0.2)
		
		local tableIndex = 2
		while currentTail[name .. index] do
			currentTail = currentTail[name .. index]
			hnd.paths[tableIndex] = currentTail
			hnd.bounces[tableIndex] = PirOdd.BounceValue()
			index = index+1
			tableIndex = tableIndex+1
		end
	else
		error("Tailroot Expected to be a modelpart or table",2)
	end
	
	table.insert(PirOdd.registered.smoothRots,hnd)
	return hnd
end

local earBase = {
	tick = function(self)
			local headRot = PirOdd.HeadOriginRot()
			local target = vec(math.map(headRot.x,-90,90,self.minPitch,self.maxPitch),math.map(headRot.y,-50,50,self.minYaw,self.maxYaw),0)
			
			local leftVel = vec(0,0,0)
			local rightVel = vec(0,0,0)
			
			local bothVel = vec(-PirOdd.localVel.z,0,PirOdd.localVel.x)*self.velIntensity
			
			if self.flickChance > 0 then
				if math.random(0,self.flickChance) == 0 then
					if math.random(0,1) == 0 then
						leftVel.z = leftVel.z + self.flickIntensity
					else
						rightVel.z = rightVel.z - self.flickIntensity
					end
				end
			end
				
			self.leftBounce:setTarget(target):updateTick(bothVel+leftVel)
			
			if self.rightElement then
				self.rightBounce:setTarget(target):updateTick(bothVel+rightVel)
			end
		end,
	render = function(self, delta)
			self.leftElement:setOffsetRot(self.leftBounce:updateRender(self.leftElement:getOffsetRot(),delta))
			if self.rightElement then
				self.rightElement:setOffsetRot(self.rightBounce:updateRender(self.rightElement:getOffsetRot(),delta))
			end
		end,

	__type = "PirOddEar"
}

earBase.__index = earBase

PirOdd.registered.ears = {}

function PirOdd.Ears(elementLeft,elementRight)
	local hnd = setmetatable({
		leftElement = elementLeft,
		rightElement = elementRight,
		
		velIntensity = 15,
		
		flickIntensity = 30,
		flickChance = 200,
		
		minYaw = -20,
		maxYaw = 20,
		
		minPitch = -40,
		maxPitch = 40,
		
		leftBounce = PirOdd.BounceValue(0.4, 0.3, nil, nil, 1, 0.4),
		rightBounce = PirOdd.BounceValue(0.4, 0.3, nil, nil, 1, 0.4),
		
		enabled = true
	}, earBase)
	
	table.insert(PirOdd.registered.ears, hnd)
	return hnd
end

local randomAnimationBase = {
	enable = function(self)
			self.lastPlay = 0
			self.enabled = true
		end,
	disable = function(self)
			self.enabled = false
		end,
	setEnabled = function(self, state)
			if state then
				self:enable()
			else
				self:disable()
			end
		end,
	tick = function(self)
			self.lastPlay = self.lastPlay+1
			if not self.animation:isPlaying() then
				if self.lastPlay > self.nextPlay then
					self.animation:play()
					self.lastPlay = 0
					self.nextPlay = math.random(self.minTime,self.maxTime)
				end
			end
		end,
		
	__type = "PirOddRandomAnimation"
}

randomAnimationBase.__index = randomAnimationBase

PirOdd.registered.RandomAnimation = {}

function PirOdd.RandomAnimation(animation, minTime, maxTime)
	assert(animation, "Animation is invalid")

	local hnd = setmetatable({
		animation = animation,
		minTime = minTime or 100,
		maxTime = maxTime or (minTime or 100) + 100,
		
		lastPlay = 0,
		nextPlay = 0,
		
		enabled = true,
	}, randomAnimationBase)
	
	table.insert(PirOdd.registered.RandomAnimation, hnd)
	
	return hnd
end

local bouncyPartBase = {
	tick = function(self)
			local posVel = PirOdd.localVel*-self.posMult
			local rotVel = vec(PirOdd.localVel.z-PirOdd.localVel.y,PirOdd.localVel.x,0)*self.rotMult
			
			rotVel.y = rotVel.y+PirOdd.bodyRotDelta*self.rotMult.y*-self.bodyRotIntensity
			
			-- extra breast-related physics
			if self.breastStuff then
				local rotTarget = vec(PirOdd.sinWave(world:getTime(100),200,5),0,0)
				
				if PirOdd.startCrouching == true then
					rotVel.x = rotVel.x-1*self.rotMult.x
				elseif PirOdd.startCrouching == false then
					rotVel.x = rotVel.x+1*self.rotMult.x
				end
				
				self.rotBounce:setTarget(rotTarget)
			end
			
			self.posBounce:updateTick(posVel)
			self.rotBounce:updateTick(rotVel)
		end,
	render = function(self, delta)
			self.part:setPos(self.posBounce:updateRender(self.part:getPos(), delta))
			self.part:setOffsetRot(self.rotBounce:updateRender(self.part:getOffsetRot(), delta))
		end,
	__type = "PirOddBouncyPart"
}

bouncyPartBase.__index = bouncyPartBase

PirOdd.registered.bouncyParts = {}

function PirOdd.BouncyPart(part, posMult, rotMult, stiffness, mass, drag)
	local hnd = setmetatable({
		part = part,
		posMult = posMult or vec(0,0,0),
		rotMult = rotMult or vec(0,0,0),
		
		bodyRotIntensity = 0.05,
		
		posBounce = PirOdd.BounceValue(0.3, stiffness or 0.13, nil, nil, mass or 0.5, 0.25),
		rotBounce = PirOdd.BounceValue(0.3, stiffness or 0.13, vec(-45,-45,-45), vec(45,45,45), mass or 0.5, 0.25),
		
		breastStuff = false,
		
		enabled = true,
	}, bouncyPartBase)
	
	table.insert(PirOdd.registered.bouncyParts, hnd)
	return hnd
end

local HeartbeatBase = {
	tick = function(self)
			if player:isLoaded() then
				local exhaustion = 0.8
				local target = 0.1
				
				local moving = player:getVelocity():length() > 0.1
				local pose = player:getPose()
				
				-- resting
				
				if player:isSneaking() and not moving then
					exhaustion = exhaustion - 0.1
					target = 0.15
				end
				
				if pose == "SLEEPING" then
					exhaustion = exhaustion + 10
					target = 0
				end
				
				if player:getVehicle() then
					exhaustion = exhaustion + 0.2
					target = 0.05
				end
				
				-- exercise
				if moving then
					exhaustion = exhaustion + 0.3
					target = target + 0.2
					
					if player:isSprinting() then
						exhaustion = exhaustion + 0.2
						target = target + 0.4
					end
				end
				
				if pose == "SWIMMING" then
					exhaustion = exhaustion + 0.2
					target = target + 0.2
				end
				
				if PirOdd.startCrouching then
					exhaustion = exhaustion + 1.2
					target = target + 0.5
				end
				
				-- item related, extreme increase so it can be noticable
				if player:getActiveItem():getID() ~= "minecraft:air" then
					exhaustion = exhaustion + 0.5
					target = target + 0.3
					
				end
				
				if player:getSwingTime() > 0 then
					exhaustion = exhaustion + 0.8
					target = target + 0.4
				end
				
				-- change target from multiplier to actual rate
				target = math.lerp(self.resting, self.maximum, target)+self.jumpMod*10
				
				-- calc vel and acc
				self.rateAcc = ((target-self.heartRate)*self.changeRate*exhaustion)*self.changeRate
				self.rateVel = (self.rateVel+self.rateAcc)*0.7
				
				-- decay jumpmod
				self.jumpMod = math.clamp((self.jumpMod-0.01)*0.99,0,10)
				
				-- modify stickyrate based on reluctance
				self.stickyRate = self.stickyRate + math.clamp(self.rateVel, -self.reluctance*0.5, self.reluctance)
				
				-- modify reluctance based on rate of change
				self.reluctance = math.lerp(self.reluctance,math.abs((target-self.heartRate)*self.changeRate*exhaustion)*0.1,self.changeRate*0.1)
				
				-- gravitate towards stickyrate
				local change = (self.stickyRate-self.heartRate)*self.changeRate*exhaustion
				
				self.heartRate = math.clamp(self.heartRate+change, self.resting, self.maximum)
				
				--host:setActionbar(math.round(self.heartRate) .. " " .. math.round(self.stickyRate) ..  " " .. self.rateVel)
				self.anim:setSpeed(self.heartRate/60)
			end
		end,
	__type = "PirOddHeartBeat"
}

HeartbeatBase.__index = HeartbeatBase

PirOdd.registered.Heartbeat = {}

function PirOdd.Heartbeat(anim)
	local hnd = setmetatable({
		anim = anim,
		resting = 50,
		maximum = 200,
		stress = 1.1,
		changeRate = 0.03,
		
		heartRate = 60,
		reluctance = 0, -- higher reluctance means changing
		stickyRate = 60,
		
		rateVel = 0,
		rateAcc = 0,
		
		jumpMod = 0,
		
		enabled = true
	},HeartbeatBase)
	
	table.insert(PirOdd.registered.Heartbeat, hnd)
	return hnd
end

local BounceWalkBase = {
	tick = function(self)
			self.multi = math.lerp(self.multi, math.min(player:getVelocity().xz:length()*5,1), 0.5)
			
			local yVel = player:getVelocity().y
			-- extra bouncy
			if self.bouncyStuff then
				local force = (self.prevVel-yVel)*0.25
				
				force = yVel*-0.1
				
				if PirOdd.startCrouching then
					force = force - 0.1
				elseif PirOdd.startCrouching == false then
					force = force + 0.1
				end
				
				self.bounce:updateTick(vec(0,force,0))
			end
			
			self.prevVel = yVel
		end,
	render = function(self, delta)
			local offset = math.abs(vanilla_model.LEFT_LEG:getOriginRot().x/50)*self.intensity*self.multi
			local squash = math.abs(vanilla_model.LEFT_LEG:getOriginRot().x/60)*self.squashIntensity*self.multi-(0.5*self.squashIntensity*self.multi)
			
			self.instantBounce = vec(1-squash,1+squash,1-squash)+self.bounce:updateRender(_,delta)
			
			self.root:setPos(0,offset,0):setScale(self.instantBounce)
		end,
	__type = "PirOddBounceWalk"
}

BounceWalkBase.__index = BounceWalkBase

PirOdd.registered.BounceWalk = {}

function PirOdd.BounceWalk(root, intensity)
	local hnd = setmetatable({
		root = root,
		intensity = intensity or 1,
		squashIntensity = 0,
		
		multi = 0.1,
		bouncyStuff = false,
		bounce = PirOdd.BounceValue(0.5, 0.55),
		prevVel = 0,
		instantBounce = vec(0,0,0),
		
		enabled =true
	},BounceWalkBase)
	
	table.insert(PirOdd.registered.BounceWalk, hnd)
	
	return hnd
end

local oldBodyRot
local oldCrouch = false
PirOdd.bodyRotDelta = 0
PirOdd.oldBodyRot = nil
PirOdd.localVel = vec(0,0,0)
PirOdd.startCrouching = nil
PirOdd.justJumped = false

function pings.jump()
	PirOdd.justJumped = true
	for _,obj in ipairs(PirOdd.registered.Heartbeat) do
		if obj.enabled then
			obj.rateVel = obj.rateVel + 0.25
			obj.reluctance = obj.reluctance + 0.1
			obj.jumpMod = obj.jumpMod +1
		end
	end
end

function events.tick()
	-- update vars
	if player:isLoaded() then
		if PirOdd.oldBodyRot then
			PirOdd.bodyRotDelta = PirOdd.oldBodyRot-player:getBodyYaw()
		end
		
		if oldCrouch ~= player:isSneaking() then
			PirOdd.startCrouching = player:isCrouching()
		else
			PirOdd.startCrouching = nil
		end
		oldCrouch = player:isCrouching()
		
		PirOdd.oldBodyRot = player:getBodyYaw()
		PirOdd.localVel = getLocalVelocity()
	end
	
	-- ping
	if host:isHost() and PirOdd.registered.Heartbeat[1] then
		if host:isJumping() and world.getBlockState(player:getPos():add(0, -0.5, 0)):isSolidBlock() and player:getVelocity().y > 0 then
			pings.jump()
		end
	end
	
	-- update all objects
	for _,obj in ipairs(PirOdd.registered.eyes) do
		if obj.enabled then
			obj:tick()
		end
	end
	for _,obj in ipairs(PirOdd.registered.wings) do
		if obj.enabled then
			obj:tick()
		end
	end
	for _,obj in ipairs(PirOdd.registered.smoothRots) do
		if obj.enabled then
			obj:tick()
		end
	end
	for _,obj in ipairs(PirOdd.registered.tails) do
		if obj.enabled then
			obj:tick()
		end
	end
	for _,obj in ipairs(PirOdd.registered.RandomAnimation) do
		if obj.enabled then
			obj:tick()
		end
	end
	for _,obj in ipairs(PirOdd.registered.bouncyParts) do
		if obj.enabled then
			obj:tick()
		end
	end
	for _,obj in ipairs(PirOdd.registered.ears) do
		if obj.enabled then
			obj:tick()
		end
	end
	for _,obj in ipairs(PirOdd.registered.Heartbeat) do
		if obj.enabled then
			obj:tick()
		end
	end
	for _,obj in ipairs(PirOdd.registered.BounceWalk) do
		if obj.enabled then
			obj:tick()
		end
	end
	PirOdd.justJumped = false
end

function events.render(delta, context)
	-- update all objects
	for _,obj in ipairs(PirOdd.registered.eyes) do
		if obj.enabled then
			obj:render(delta,context)
		end
	end
	for _,obj in ipairs(PirOdd.registered.wings) do
		if obj.enabled then
			obj:render(delta,context)
		end
	end
	for _,obj in ipairs(PirOdd.registered.smoothRots) do
		if obj.enabled then
			obj:render(delta,context)
		end
	end
	for _,obj in ipairs(PirOdd.registered.tails) do
		if obj.enabled then
			obj:render(delta,context)
		end
	end
	for _,obj in ipairs(PirOdd.registered.bouncyParts) do
		if obj.enabled then
			obj:render(delta,context)
		end
	end
	for _,obj in ipairs(PirOdd.registered.ears) do
		if obj.enabled then
			obj:render(delta,context)
		end
	end
	for _,obj in ipairs(PirOdd.registered.BounceWalk) do
		if obj.enabled then
			obj:render(delta,context)
		end
	end
end

return PirOdd