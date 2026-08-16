local anim = animations.rofdragon
local model = models.rofdragon
require("GSAnimBlend")

local flytable = {
	anim.flap,
	anim.left,
	anim.right,
	anim.dive,
	anim.glide,
	anim.glideup,
	anim.airstall,
	anim.stallflap,
	anim.glidepose
}

flytable[1]:setBlendTime(7)
flytable[2]:setBlendTime(15)
flytable[3]:setBlendTime(15)
flytable[4]:setBlendTime(8)
flytable[5]:setBlendTime(10)
flytable[6]:setBlendTime(10)
flytable[7]:setBlendTime(8)
flytable[8]:setBlendTime(8)
flytable[9]:setBlendTime(5)

function events.tick()
	if player:isGliding() then
		model.root:setRot(90, 0, 0)
		else
		model.root:setRot(0, 0, 0)
		flytable[1]:stop()
		flytable[2]:stop()
		flytable[3]:stop()
		flytable[4]:stop()
		flytable[5]:stop()
		flytable[6]:stop()
		flytable[7]:stop()
		flytable[8]:stop()
		flytable[9]:stop()
	end
end

--glidepose
function events.tick()
	if player:isGliding() then
		flytable[9]:play()
	else flytable[9]:stop()
	end
end

--dive
function events.tick()
	if player:getLookDir().y < -0.85 and player:isGliding() and not flytable[7]:isPlaying() and not flytable[8]:isPlaying() then
			flytable[4]:play()
		else 
			flytable[4]:stop()
		end
end

--glide
function events.tick()
	flytable[5]:setPlaying(player:isGliding() and not flytable[1]:isPlaying() and not flytable[4]:isPlaying() and not flytable[8]:isPlaying() and not flytable[7]:isPlaying() and player:getLookDir().y < 0.2)
end

--glideup
function events.tick()
	flytable[6]:setPlaying(player:isGliding() and not flytable[1]:isPlaying() and not flytable[4]:isPlaying() and not flytable[8]:isPlaying() and not flytable[7]:isPlaying() and not flytable[5]:isPlaying() and player:getLookDir().y > 0.2)
end

--flap keybind
local  flappingkeyState = false
function pings.flap(wingsState)
    if not player:isLoaded() then return end
	flappingkeyState = wingsState
end
local flapKey = keybinds:newKeybind("Wings", "key.keyboard.left.control", false)
flapKey.press = function()
    if player:isLoaded() and player:isGliding() and flytable[4]:isPlaying() == false then pings.flap(true) 
	elseif player:isGliding(false) then pings.flap(false)
	end
end
flapKey.release = function()
    pings.flap(false)
end

function events.tick()
	if player:isGliding() and not player:isSneaking() and not flytable[4]:isPlaying() then 
		flytable[1]:setPlaying(flappingkeyState)
	elseif player:isGliding() and player:isSneaking() and not flytable[4]:isPlaying() then
		flytable[8]:setPlaying(flappingkeyState)
	end
end

--airstall
function events.tick()
	if player:isSneaking() and player:isGliding() and not flytable[4]:isPlaying() and not flytable[8]:isPlaying() then
		flytable[7]:play()
	else flytable[7]:stop()
	end
end

--lefttilt
function events.tick()
	if player:isGliding() and vanilla_model.HEAD:getOriginRot().y > 18 and vanilla_model.HEAD:getOriginRot().y < 51 and not flytable[1]:isPlaying() and not flytable[4]:isPlaying() and not flytable[8]:isPlaying() and not flytable[7]:isPlaying() then
		flytable[2]:play()
		else flytable[2]:stop()
	end
end

--righttilt
function events.tick()
	if player:isGliding() and vanilla_model.HEAD:getOriginRot().y <  -18 and vanilla_model.HEAD:getOriginRot().y >  -51 and not flytable[1]:isPlaying() and not flytable[4]:isPlaying() and not flytable[8]:isPlaying() and not flytable[7]:isPlaying() then
		flytable[3]:play()
		else flytable[3]:stop()
	end
end


function events.tick()
	if not player:isSneaking() then
		flytable[8]:stop()
		flytable[7]:stop()
	end
end

--stop lower priority anims from playing
---dive
function events.tick()
	if flytable[4]:isPlaying() then
		flytable[1]:stop()
		flytable[7]:stop()
		flytable[8]:stop()
		flytable[5]:stop()
		flytable[2]:stop()
		flytable[3]:stop()
		flytable[6]:stop()
	end
end

---stallflap
function events.tick()
	if flytable[8]:isPlaying() then
		flytable[1]:stop()
		flytable[7]:stop()
		flytable[5]:stop()
		flytable[2]:stop()
		flytable[3]:stop()
		flytable[6]:stop()
	end
end

---airstall
function events.tick()
	if flytable[7]:isPlaying() then
		flytable[1]:stop()
		flytable[5]:stop()
		flytable[2]:stop()
		flytable[3]:stop()
		flytable[6]:stop()
	end
end

--flap
function events.tick()
	if flytable[1]:isPlaying() then
		flytable[5]:stop()
		flytable[2]:stop()
		flytable[3]:stop()
		flytable[6]:stop()
	end
end