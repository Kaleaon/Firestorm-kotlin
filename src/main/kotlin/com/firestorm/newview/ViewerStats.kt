package com.firestorm.newview

object ViewerStats {

    object StatNames {
        const val FPS                          = "fps"
        const val SIM_FPS                      = "sim_fps"
        const val PHYSICS_FPS                  = "physics_fps"
        const val AGENT_UPS                    = "agent_ups"
        const val SCRIPT_EPS                   = "script_eps"
        const val PACKET_LOSS                  = "packet_loss"
        const val PACKETS_IN                   = "packets_in"
        const val PACKETS_LOST                 = "packets_lost"
        const val PACKETS_OUT                  = "packets_out"
        const val BANDWIDTH                    = "bandwidth"
        const val PING                         = "ping"
        const val OBJECTS_DRAWN                = "objects_drawn"
        const val TRIANGLES_DRAWN              = "triangles_drawn"
        const val TEXTURE_MEMORY               = "texture_memory"
        const val VERTEX_MEMORY                = "vertex_memory"
        const val NUM_IMAGES                   = "num_images"
        const val NUM_RAW_IMAGES               = "num_raw_images"
        const val NUM_OBJECTS                  = "num_objects"
        const val NUM_ACTIVE_OBJECTS           = "num_active_objects"
        const val NUM_MATERIALS                = "num_materials"
        const val VISIBLE_AVATARS              = "visible_avatars"
        const val DRAW_DISTANCE                = "draw_distance"
        const val FRAME_TIME                   = "frame_time"
        const val SIM_FRAME_TIME               = "sim_frame_time"
        const val SIM_NET_TIME                 = "sim_net_time"
        const val SIM_OTHER_TIME               = "sim_other_time"
        const val SIM_PHYSICS_TIME             = "sim_physics_time"
        const val SIM_PHYSICS_STEP_TIME        = "sim_physics_step_time"
        const val SIM_PHYSICS_SHAPE_UPDATE_TIME= "sim_physics_shape_update_time"
        const val SIM_PHYSICS_OTHER_TIME       = "sim_physics_other_time"
        const val SIM_AI_TIME                  = "sim_ai_time"
        const val SIM_AGENTS_TIME              = "sim_agents_time"
        const val SIM_IMAGES_TIME              = "sim_images_time"
        const val SIM_SCRIPTS_TIME             = "sim_scripts_time"
        const val SIM_SPARE_TIME               = "sim_spare_time"
        const val SIM_SLEEP_TIME               = "sim_sleep_time"
        const val SIM_PUMP_IO_TIME             = "sim_pump_io_time"
        const val SIM_TIME_DILATION            = "sim_time_dilation"
        const val SIM_MAIN_AGENTS              = "sim_main_agents"
        const val SIM_CHILD_AGENTS             = "sim_child_agents"
        const val SIM_OBJECTS                  = "sim_objects"
        const val SIM_ACTIVE_OBJECTS           = "sim_active_objects"
        const val SIM_ACTIVE_SCRIPTS           = "sim_active_scripts"
        const val SIM_IN_PACKETS_PER_SEC       = "sim_in_packets_per_sec"
        const val SIM_OUT_PACKETS_PER_SEC      = "sim_out_packets_per_sec"
        const val SIM_PENDING_DOWNLOADS        = "sim_pending_downloads"
        const val SIM_PENDING_UPLOADS          = "sim_pending_uploads"
        const val SIM_PENDING_LOCAL_UPLOADS    = "sim_pending_local_uploads"
        const val SIM_PHYSICS_PINNED_TASKS     = "sim_physics_pinned_tasks"
        const val SIM_PHYSICS_LOD_TASKS        = "sim_physics_lod_tasks"
        const val SIM_UNACKED_BYTES            = "sim_unacked_bytes"
        const val SIM_PHYSICS_MEM              = "sim_physics_mem"
        const val FRAMETIME_JITTER             = "frametime_jitter"
        const val REGION_CROSSING_TIME         = "region_crossing_time"
        const val AVATAR_EDIT_TIME             = "avatar_edit_time"
        const val TOOLBOX_TIME                 = "toolbox_time"
        const val MOUSELOOK_TIME               = "mouselook_time"
        const val OBJECT_CACHE_HIT_RATE        = "object_cache_hit_rate"
        const val PERCENTAGE_SCRIPTS_RUN       = "percentage_scripts_run"
        const val SKIPPED_CHARACTERS_PERCENTAGE= "skipped_characters_percentage"
        const val TEX_BAKES                    = "tex_bakes"
        const val TEX_REBAKES                  = "tex_rebakes"
        const val LOADING_WEARABLES_LONG_DELAY = "loading_wearables_long_delay"
        const val CHAT_COUNT                   = "chat_count"
        const val IM_COUNT                     = "im_count"
        const val OBJECT_CREATE                = "object_create"
        const val OBJECT_REZ                   = "object_rez"
        const val LOGIN_TIMEOUTS               = "login_timeouts"
        const val ANIMATION_UPLOADS            = "animation_uploads"
        const val SNAPSHOT                     = "snapshot"
        const val UPLOAD_SOUND                 = "upload_sound"
        const val UPLOAD_TEXTURE               = "upload_texture"
        const val EDIT_TEXTURE                 = "edit_texture"
        const val KILLED                       = "killed"
        const val TELEPORT                     = "teleport"
        const val FLY                          = "fly"
    }

    private val stats: MutableMap<String, Double> = mutableMapOf()

    var fps: Float            = 0f
    var simFps: Float         = 0f
    var packetLoss: Float     = 0f
    var bandwidth: Float      = 0f
    var ping: Float           = 0f
    var objectsDrawn: Int     = 0
    var trianglesDrawn: Long  = 0L
    var textureMemory: Long   = 0L
    var vertexMemory: Long    = 0L

    fun update(dt: Float) {}

    fun getStat(name: String): Double = when (name) {
        StatNames.FPS             -> fps.toDouble()
        StatNames.SIM_FPS         -> simFps.toDouble()
        StatNames.PACKET_LOSS     -> packetLoss.toDouble()
        StatNames.BANDWIDTH       -> bandwidth.toDouble()
        StatNames.PING            -> ping.toDouble()
        StatNames.OBJECTS_DRAWN   -> objectsDrawn.toDouble()
        StatNames.TRIANGLES_DRAWN -> trianglesDrawn.toDouble()
        StatNames.TEXTURE_MEMORY  -> textureMemory.toDouble()
        StatNames.VERTEX_MEMORY   -> vertexMemory.toDouble()
        else                      -> stats[name] ?: 0.0
    }

    fun setStat(name: String, value: Double) {
        when (name) {
            StatNames.FPS             -> fps           = value.toFloat()
            StatNames.SIM_FPS         -> simFps        = value.toFloat()
            StatNames.PACKET_LOSS     -> packetLoss    = value.toFloat()
            StatNames.BANDWIDTH       -> bandwidth     = value.toFloat()
            StatNames.PING            -> ping          = value.toFloat()
            StatNames.OBJECTS_DRAWN   -> objectsDrawn  = value.toInt()
            StatNames.TRIANGLES_DRAWN -> trianglesDrawn= value.toLong()
            StatNames.TEXTURE_MEMORY  -> textureMemory = value.toLong()
            StatNames.VERTEX_MEMORY   -> vertexMemory  = value.toLong()
            else                      -> stats[name]   = value
        }
    }
}
