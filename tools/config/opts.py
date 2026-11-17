# (section, side, pt_title, en_title, pt_comment, en_comment, [options])
# option: (key, FIELD, kind, default, min, max, pt_label, en_label, pt_tip, en_tip)
B,I,D='bool','int','double'
SECTIONS = [
("general","server","Geral","General","Regras gerais do Runeheart","General Runeheart rules",[
 ("debug_log","DEBUG_LOG",B,False,0,1,"Logs de diagnóstico","Diagnostic logs","true = escreve no latest.log o que o Runeheart está fazendo (marcador [Runeheart]), para investigar problemas","true = writes what Runeheart is doing to latest.log (marker [Runeheart]), to investigate problems"),
]),
("visual","client","Visual e HUD","Visuals and HUD","Visual e HUD (só neste computador)","Visuals and HUD (this computer only)",[
 ("show_hud","SHOW_HUD",B,True,0,1,"Mostrar o HUD de mana natural","Show the natural mana HUD","Mostrar o medidor de mana natural quando o Mana Sense estiver ativo","Show the natural mana gauge while Mana Sense is active"),
]),
]
