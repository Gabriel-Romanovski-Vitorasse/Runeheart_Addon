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
("absorption","server","Absorção","Absorption","Absorção: segurando a tecla de concentração, parado, o jogador puxa a mana natural do lugar para recarregar a própria mana","Absorption: holding the focus key while standing still, the player draws the place's natural mana to refill their own",[
 ("enabled","ABS_ENABLED",B,True,0,1,"Ligada","Enabled","Liga/desliga a absorção","Turns absorption on/off"),
 ("mana_percent_per_second","ABS_MANA_PERCENT",D,3.0,0.0,100.0,"Mana por segundo (% do máximo)","Mana per second (% of max)","Mana recuperada por segundo com densidade 100, em porcentagem da mana máxima. Cai junto com a densidade (densidade 50 = metade)","Mana regained per second at density 100, as a percentage of max mana. Scales down with density (density 50 = half)"),
 ("natural_cost_per_second","ABS_NATURAL_COST",D,2.0,0.0,100.0,"Mana natural gasta por segundo","Natural mana used per second","Quanto a densidade do chunk cai por segundo de absorção (enquanto a mana do jogador está enchendo)","How much the chunk's density drops per second of absorption (while the player's mana is filling)"),
 ("overload_below","ABS_OVERLOAD_BELOW",I,10,0,100,"Sobrecarga abaixo de","Overload below","Abaixo dessa densidade, concentrar não rende mana: causa náusea e dano","Below this density, focusing gives no mana: it causes nausea and damage"),
 ("overload_damage","ABS_OVERLOAD_DAMAGE",D,1.0,0.0,20.0,"Dano da sobrecarga","Overload damage","Dano a cada 2 segundos de sobrecarga","Damage every 2 seconds of overload"),
 ("move_tolerance","ABS_MOVE_TOLERANCE",D,0.15,0.0,5.0,"Tolerância de movimento","Movement tolerance","Quanto o jogador pode se mexer por tick (em blocos) sem quebrar a concentração","How far the player may move per tick (in blocks) without breaking focus"),
 ("require_attunement","ABS_REQUIRE_ATTUNEMENT",B,True,0,1,"Exigir sintonia","Require attunement","true = antes de absorver, o jogador precisa 'sentir a mana natural': ficar parado com o Mana Sense ligado num lugar denso","true = before absorbing, the player must 'sense natural mana': stand still with Mana Sense on in a dense place"),
 ("attunement_seconds","ABS_ATTUNEMENT_SECONDS",I,120,0,36000,"Tempo de sintonia (s)","Attunement time (s)","Segundos (somados, não precisam ser seguidos) parado com o Mana Sense ligado num lugar denso para aprender a absorção","Seconds (added up, not necessarily in a row) standing still with Mana Sense on in a dense place to learn absorption"),
 ("attunement_density","ABS_ATTUNEMENT_DENSITY",I,60,0,100,"Densidade para a sintonia","Attunement density","Densidade mínima do lugar para a sintonia avançar","Minimum density for attunement to progress"),
]),
("visual","client","Visual e HUD","Visuals and HUD","Visual e HUD (só neste computador)","Visuals and HUD (this computer only)",[
 ("hud_x","HUD_X",I,6,0,4000,"Posição X do HUD","HUD X position","Distância da borda esquerda da tela, em pixels da interface","Distance from the left edge of the screen, in GUI pixels"),
 ("hud_y","HUD_Y",I,6,0,4000,"Posição Y do HUD","HUD Y position","Distância do topo da tela, em pixels da interface","Distance from the top of the screen, in GUI pixels"),
 ("particles","PARTICLES",B,True,0,1,"Partículas em lugares densos","Particles in dense places","Mostrar um pólen brilhante no ar onde a mana natural é alta","Show glowing pollen in the air where natural mana is high"),
 ("particle_threshold","PARTICLE_THRESHOLD",I,70,0,100,"Densidade para as partículas","Density for particles","Densidade a partir da qual as partículas aparecem","Density from which the particles appear"),
 ("show_hud","SHOW_HUD",B,True,0,1,"Mostrar o HUD de mana natural","Show the natural mana HUD","Mostrar o medidor de mana natural quando o Mana Sense estiver ativo","Show the natural mana gauge while Mana Sense is active"),
]),
]
