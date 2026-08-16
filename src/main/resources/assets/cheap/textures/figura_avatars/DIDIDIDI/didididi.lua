vanilla_model.PLAYER:visible(false)
vanilla_model.ARMOR:visible(false)
vanilla_model.PARROTS:visible(false)
vanilla_model.ELYTRA:visible(false)
vanilla_model.CAPE:visible(false)
nameplate.Entity:pivot(0, 1.3, 0):light(15):backgroundColor(0, 0, 0, 0):shadow(true)

---

local sequence = {
    1,2,3,4,5,6,7,8,9,
    1,2,3,4,5,6,7,8,9,
    
    10,11,12,
    
    13,14,15,16,17,
    15,14,13,12,
    13,14,15,16,17,
    15,14,13,12,
    13,14,15,16,17,
    15,14,13,12,
    11,10,

    1,2,3,4,5,6,7,8,9,
    1,2,3,4,5,6,7,8,9,

    19,
    18,20,18,19,
    -18,-20,-18,-19,
    18,20,18,19,
    -18,-20,-18,-19,
    18,20,18,19,
    -18,-20,-18,-19,
    18,20,18,19,
    -18,-20,-18,-19,

    1,2,3,4,5,6,7,8,9,
    1,2,3,4,5,6,7,8,9,
    
    22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,
    37,36,35,34,33,32,31,30,29,28,27,26,25,24,23,22,

    1,2,3,4,5,6,7,8,9,
    1,2,3,4,5,6,7,8,9,
    
    41,40,39,40,41,40,39,40,41,40,39,40,41,40,39
}

local M = models.didididi
local Body = M.Body
Body:light(15)

local di = Body.di
local didi = Body.didi

local abs = math.abs

local function DIDIDIDI()
    local frame = sequence[(world.getTime() % #sequence) + 1]
    local absframe = abs(frame)

    Body:uv(_, absframe / 42)

    local dididi = frame < 0
    di:visible(absframe ~= 41 and not dididi)
    didi:visible(absframe == 41 or dididi)
end

function events.entity_init()
    events.TICK:register(DIDIDIDI)
end

function pings.Dancing(state)
    if state then
        events.TICK:register(DIDIDIDI)
    else
        events.TICK:remove(DIDIDIDI)
    end
end

local keybindState = true
keybinds:newKeybind("ᴅᴀɴᴄɪɴɢ", "key.keyboard.i")
    .press = function()
        keybindState = not keybindState
        pings.Dancing(keybindState)
end

---

local wasThing = false
function events.render()
    local p_crouching = player:isCrouching()

    if wasThing ~= p_crouching then
        wasThing = p_crouching
        if p_crouching then
            M:pos(0, 4.14, -4)
        else
            M:pos()
        end
    end
end
