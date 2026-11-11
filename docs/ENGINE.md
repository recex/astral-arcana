# Astral Engine — motor gráfico do Astral Arcana (Forge 1.20.1)

Evolução do mod existente (nenhum mod novo): mesmo Minecraft 1.20.1, mesmo Forge 47.3.0, mapeamentos oficiais, sem dependências novas.

## 1. Auditoria do projeto original (v1.x) — o que foi encontrado

| Achado | Onde | Consequência |
|---|---|---|
| `ClientFx.java` monolítico (~1.700 linhas): estado, partículas, HUD, câmera e pós-processo misturados | client | difícil de evoluir e testar |
| Partículas criadas direto por `particleEngine.add` sem orçamento, sem escala de qualidade e sem LOD | `glow/orbit/orbitRise` | supernova/impactos criavam centenas por tick, até em máquinas fracas |
| Pós-processo de 1 passe só (distorção em tela), com cópia do framebuffer inteiro a cada frame e **sem** profundidade | `onRenderLevelEnd` | nada de efeitos baseados em profundidade, sem bloom |
| `copyTarget` nunca liberado (nem ao sair do mundo, nem no reload de recursos) | `ClientFx` | vazamento de memória de GPU |
| Falha de compilação de shader derrubava o carregamento | `ClientSetup` | um erro de GLSL = crash |
| `shouldRender` retornava `true` sempre (sem frustum culling) em todos os renderers | `*Renderer` | geometria desenhada fora da tela |
| Malhas (esferas) com tesselação fixa 12×20 independentemente da distância | `FxRender` | desperdício de vértices |
| Tremor de câmera por senoides fixas, sem camadas, sem timeline | `onCamera` | sem sequências cinematográficas |
| Áudio só no servidor, sons soltos | `Spells` | sem camadas, sem sincronia |
| Nenhuma configuração do usuário | — | impossível ajustar desempenho |

Preservado: todos os 31 poderes, entidades, rede, receitas, texturas, o shader `distort` (virou *fallback*).

## 2. Arquitetura (`com.astralarcana.engine`)

```
engine/
  core/       Engine (ciclo), EngineConfig + Settings (config do cliente), Quality, EngineEvents, EngineCommands
  perf/       PerformanceEngine  – FPS médio, qualidade adaptativa, LOD, frustum culling
  shader/     ShaderEngine (registro/erros/recarga), ShaderProgram (cache de uniforms),
              FramebufferPool, ScreenQuad, PostPipeline (cena + bloom), PostParams
  particle/   ParticleSystem     – orçamento por tick, escala de qualidade, LOD por distância
  anim/       Easing, Bezier, Track  – curvas (Java puro, com testes)
  cinematic/  CinematicEngine, Sequence (timeline declarativa), Sequences (biblioteca)
  audio/      AudioEngine (cues em camadas), FadingSound (volume/pitch por curva, 3D)
  vfx/        VFXEngine + RibbonEffect, PortalEffect, EnergyFieldEffect, GroundCrackEffect, Debris, Fx
  world/      WorldVisualEngine  – luzes dinâmicas, ondas 3D, neblina, orbes volumétricos
```

### Fluxo de um frame
1. `ClientFx` (eventos Forge) junta os parâmetros de tela e chama `PostPipeline.run` em `AFTER_LEVEL`.
2. **Passe de cena** (`cinematic.fsh`): copia cor e profundidade → um único passe aplica distorção (lente, onda, redemoinho, glitch), motion blur por reprojeção, luzes dinâmicas, ondas 3D, orbes volumétricos (raymarching com oclusão pela profundidade), neblina, color grading.
3. **Bloom**: extração em resolução reduzida → blur gaussiano separável → soma aditiva.
4. Se `cinematic` não compilar, usa-se o `distort` antigo; se nada compilar, os efeitos de tela desligam e o jogo segue.

## 3. Recursos implementados

- **Shaders GLSL** (`assets/astralarcana/shaders/core`): `cinematic`, `distort` (fallback), `bloom_extract`, `blur`, `bloom_composite`. Uniforms validados contra os JSON (nomes, tipos, contagens).
- **Profundidade**: posição reconstruída por `InvPV`; usada em luzes, ondas 3D, neblina, volumétricos e motion blur.
- **Orbes volumétricos** com ruído fBm procedural e "faíscas" por célula (partículas computadas na GPU, sem custo de CPU). Tipos: fogo/nebulosa e vazio (buraco negro).
- **Ribbons/trails** com suavização Catmull-Rom (meteoros, cabeça e cauda da serpente), **portais** (salto/troca), **campo de energia** em grade (Barreira Estelar), **rachaduras no chão** que seguem o relevo, **destroços** com partículas do bloco real.
- **Cinemática**: tremor por ruído suave em camadas, sequências por timeline (câmera yaw/pitch/roll/FOV, barras, fade, eventos de VFX/áudio/shader). Sequências prontas: `supernova`, `collapse`, `roar`, `timestop`, `showcase`.
- **Animação**: 18 easings, Bezier estilo CSS, tracks por keyframe.
- **Áudio**: cues em camadas com atraso, glissando de pitch, atraso por distância (velocidade do som), volume configurável.
- **Mundo**: tudo é visual; nenhum bloco é alterado pelo motor (rachaduras/destroços são só renderização/partículas).
- **Desempenho**: qualidade adaptativa (LOW…ULTRA, por FPS médio), orçamento de partículas, LOD de malhas, frustum culling nos renderers e nos efeitos, pool de framebuffers com liberação ao sair do mundo e no reload, bloom/pós só quando há efeito ativo.

## 4. Configuração e comandos

`config/astralarcana-client.toml`: `quality`, `adaptiveQuality`, `targetFps`, `postProcessing`, `bloom`, `bloomIntensity`, `volumetrics`, `dynamicLights`, `motionBlur`, `shakeScale`, `audioVolume`.

```
/astral stats                 FPS médio, qualidade, partículas/tick, efeitos, framebuffers
/astral shaders               estado de cada programa GLSL e mensagens de erro
/astral reload                recarrega recursos e shaders (F3+T também funciona)
/astral quality <auto|low|medium|high|ultra>
/astral cinematic <supernova|collapse|roar|timestop|showcase>
```

## 5. Criar uma sequência

```java
Sequence s = Sequence.named("minha", 60)
    .at(0, c -> { Fx.flash(Fx.WHITE, 1f * c.k, 0.03f); WorldVisualEngine.addLight(c.origin, Fx.CYAN, 30f, 2f, 30); })
    .shake(0, 2f, 30)
    .sound(0, AudioEngine.Cues.IMPACT, 1f)
    .key(Sequence.Prop.FOV, 0, 0f, Easing.LINEAR).key(Sequence.Prop.FOV, 4, 0.3f, Easing.OUT_EXPO).key(Sequence.Prop.FOV, 40, 0f, Easing.IN_OUT_SINE)
    .build();
CinematicEngine.play(s, origem, 1f);
```

## 6. Validação — o que foi e o que NÃO foi verificado

Verificado neste ambiente (sem Minecraft/Forge/GPU):
- `javac` real sobre todo o código: **0 erros de sintaxe**.
- Classes puras (`Easing`, `Bezier`, `Track`) compiladas e testadas (valores nos extremos, monotonicidade, finitude).
- `PerformanceEngine` e `WorldVisualEngine` compilados com stubs mínimos e testados: qualidade adaptativa desce/sobe e respeita o teto da config; LOD; luzes/orbes em coordenadas relativas à câmera; seleção das N mais importantes por qualidade; expiração e itens "hold"; limite de neblina.
- Matemática do shader validada numericamente (reconstrução de posição por profundidade, reprojeção do motion blur, raio do horizonte).
- Referências entre classes do projeto, contagem de argumentos e imports conferidos; uniforms GLSL ↔ JSON ↔ Java conferidos.

**Não verificado** (precisa rodar o jogo): a compilação completa contra o Forge (rode o GitHub Actions), a compilação do GLSL no driver da sua GPU (use `/astral shaders`), o visual final e o desempenho real.

## 7. Limitações técnicas e decisões

- **Instanced rendering / compute / transform feedback não foram usados**: exigiriam contornar o estado de renderização do Minecraft e o contexto OpenGL 3.2 core; no lugar, partículas procedurais na GPU (no shader) + geometria agrupada em batch.
- **Câmera**: a API do Forge 1.20.1 permite ângulos e FOV, não posição; "travelling" é simulado com FOV/roll.
- **Luz dinâmica** é re-iluminação em espaço de tela (sem normais); não altera o motor de luz do mundo.
- **Áudio**: só sons do jogo combinados/alterados em pitch; o mod não embute arquivos .ogg.
- **Shaderpacks (Iris/OptiFine)** podem desativar ou substituir o pós-processo do mod.
