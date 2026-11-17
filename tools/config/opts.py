# (section, side, pt_title, en_title, pt_comment, en_comment, [options])
# option: (key, FIELD, kind, default, min, max, pt_label, en_label, pt_tip, en_tip)
B,I,D='bool','int','double'
SECTIONS = [
("general","server","Geral","General","Regras gerais do Runeheart","General Runeheart rules",[
 ("debug_log","DEBUG_LOG",B,False,0,1,"Logs de diagnóstico","Diagnostic logs","true = escreve no latest.log o que o Runeheart está fazendo (marcador [Runeheart]), para investigar problemas","true = writes what Runeheart is doing to latest.log (marker [Runeheart]), to investigate problems"),
]),
("natural_mana","server","Mana natural","Natural mana","Mana natural do ambiente: cada chunk tem uma densidade de 0 a 100 conforme o bioma, a vegetação, a água, o clima e as construções","Natural mana in the environment: every chunk has a density from 0 to 100 based on biome, vegetation, water, weather and buildings",[
 ("enabled","NM_ENABLED",B,True,0,1,"Ligada","Enabled","Liga/desliga a mana natural","Turns natural mana on/off"),
 ("regen_seconds","NM_REGEN_SECONDS",I,300,10,36000,"Tempo para encher (s)","Refill time (s)","Segundos para um chunk esgotado voltar à densidade cheia","Seconds for a drained chunk to refill completely"),
 ("rescan_seconds","NM_RESCAN_SECONDS",I,300,10,36000,"Reavaliar o terreno (s)","Rescan terrain (s)","De quanto em quanto tempo a vegetação e as construções de um chunk são contadas de novo","How often a chunk's vegetation and buildings are counted again"),
 ("vegetation_bonus","NM_VEGETATION_BONUS",D,20.0,0.0,100.0,"Bônus da vegetação","Vegetation bonus","Densidade somada por um chunk coberto de vegetação (folhas, troncos, flores, plantas)","Density added by a chunk covered in vegetation (leaves, logs, flowers, plants)"),
 ("water_bonus","NM_WATER_BONUS",D,10.0,0.0,100.0,"Bônus da água corrente","Flowing water bonus","Densidade somada quando o chunk tem água corrente ou cachoeira","Density added when the chunk has flowing water or a waterfall"),
 ("storm_bonus","NM_STORM_BONUS",D,15.0,0.0,100.0,"Bônus da tempestade","Storm bonus","Densidade somada durante tempestades com raios","Density added during thunderstorms"),
 ("building_penalty","NM_BUILDING_PENALTY",D,30.0,0.0,100.0,"Perda pelas construções","Building penalty","Densidade tirada de um chunk tomado por construções (tijolos, tábuas, vidro, ferro, redstone)","Density removed from a chunk taken over by buildings (bricks, planks, glass, iron, redstone)"),
]),
("visual","client","Visual e HUD","Visuals and HUD","Visual e HUD (só neste computador)","Visuals and HUD (this computer only)",[
 ("hud_x","HUD_X",I,6,0,4000,"Posição X do HUD","HUD X position","Distância da borda esquerda da tela, em pixels da interface","Distance from the left edge of the screen, in GUI pixels"),
 ("hud_y","HUD_Y",I,6,0,4000,"Posição Y do HUD","HUD Y position","Distância do topo da tela, em pixels da interface","Distance from the top of the screen, in GUI pixels"),
 ("particles","PARTICLES",B,True,0,1,"Partículas em lugares densos","Particles in dense places","Mostrar um pólen brilhante no ar onde a mana natural é alta","Show glowing pollen in the air where natural mana is high"),
 ("particle_threshold","PARTICLE_THRESHOLD",I,70,0,100,"Densidade para as partículas","Density for particles","Densidade a partir da qual as partículas aparecem","Density from which the particles appear"),
 ("show_hud","SHOW_HUD",B,True,0,1,"Mostrar o HUD de mana natural","Show the natural mana HUD","Mostrar o medidor de mana natural quando o Mana Sense estiver ativo","Show the natural mana gauge while Mana Sense is active"),
]),
]
