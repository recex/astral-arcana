# Astral Arcana (Forge 1.20.1)

Magia cinematográfica: o **Cajado Cósmico** tem 31 magias.

- **Segure o botão direito** para carregar (círculo mágico, zoom, barras de cinema), **solte** para lançar.
- **Shift + botão direito** troca de magia. Olhando para cima volta uma magia; olhando para baixo avança 5.

| # | Magia | O que faz |
|---|---|---|
| 1 | Chuva de Meteoros | Meteoros em chamas caem em diagonal, com rastro, onda de choque e tremor de tela |
| 2 | Singularidade | Buraco negro com lente gravitacional, disco de acreção e gravidade; colapsa no fim |
| 3 | Raio Astral | Raio de energia que atravessa inimigos e explode no destino |
| 4 | Supernova | Explosão cósmica ao redor de você: flash branco, esferas e anéis de choque |
| 5 | Tornado Astral | Redemoinho que avança, levanta e arremessa tudo; o shader torce a tela em espiral |
| 6 | Parar o Tempo | Congela todos ao redor; tela em preto e branco, azulada, com relógio mágico |
| 7 | Salto Dimensional | Teletransporte instantâneo em linha reta com túnel de luz, blur radial e FOV |
| 8 | Ataque Orbital | Marca o chão e depois cai uma chuva de pilares de luz; o do centro é gigante |
| 9 | Raio em Cadeia | Relâmpago que pula entre inimigos próximos, com glitch na tela |
| 10 | Corte Dimensional | Corte em arco que racha a tela (glitch) e arremessa quem está na frente |
| 11 | Renascimento Estelar | Cura você e aliados, remove efeitos ruins, coluna de estrelas douradas |
| 12 | Colapso Gravitacional | Puxa todos para um ponto e esmaga |
| 13 | Nova de Gelo | Congela e retarda todos ao redor; tela gelada e dessaturada |
| 14 | Nova Solar | Explosão de fogo ao redor, com onda de calor |
| 15 | Grito Sônico | Onda em cone que arremessa tudo à frente |
| 16 | Rajada Estelar | 5 raios em leque |
| 17 | Julgamento dos Céus | Raios caem do céu sobre os inimigos próximos |
| 18 | Troca Dimensional | Troca de lugar com o alvo |
| 19 | Impulso Estelar | Dash com super velocidade e FOV esticado |
| 20 | Visão Estelar | Faz todos brilharem e dá visão noturna |
| 21 | Meteoro Colossal | Um meteoro gigante |
| 22 | Trio de Singularidades | Três buracos negros menores |
| 23 | Raio Trator | Puxa o alvo até você |
| 24 | Fúria Estelar | Força, velocidade e resistência, tela avermelhada |
| 25 | Véu Estelar | Invisibilidade com tela dessaturada e glitch |
| 26 | Barreira Estelar | Domo que repele tudo (zona) |
| 27 | Ciclone de Lâminas | Lâminas giratórias ao seu redor (zona) |
| 28 | Tempestade Elétrica | Raios caindo numa área por 6 s (zona) |
| 29 | Nevasca Ártica | Neve, lentidão e congelamento numa área (zona) |
| 30 | Terremoto | Tremor forte que lança todos para cima (zona) |
| 31 | Santuário Estelar | Área que cura jogadores e fere monstros (zona) |

Receita: Estrela do Nether + 2 Varas de Blaze (vertical).

## Serpente Cósmica (chefe)
Ovo de geração no menu criativo, ou `/summon astralarcana:cosmic_serpent`.
Corpo de 36 segmentos que seguem a cabeça, ondulando, com espinhos, anéis de luz e estrelas na pele.
Ataques: voo em círculos, mergulho, sopro de raio astral e chuva de meteoros. Dropa Estrela do Nether.
Só a cabeça pode ser atingida; o corpo causa dano ao encostar.

## Shader de distorção de tela
Lente gravitacional (buraco negro), ondas de choque, redemoinho (tornado), glitch (raio e corte),
dessaturação/inversão (parar o tempo), vinheta, blur radial e aberração cromática.
Não funciona com shaderpacks (Iris/OptiFine) ativos.

## Compilar
- **GitHub Actions**: faça push; o .jar aparece em *Actions → Build mod → Artifacts*.
  Para release: `git tag v1.0.0 && git push --tags`.
- Local: `gradle build` (Gradle 8.1.1, Java 17). O .jar sai em `build/libs/`.

Os meteoros e explosões respeitam a gamerule `mobGriefing`.

## Astral Engine (motor gráfico)
Shaders GLSL com profundidade (luzes dinâmicas, ondas 3D, neblina, orbes volumétricos por raymarching, motion blur),
bloom, portais, ribbons, rachaduras no chão, cinemática por timeline, áudio em camadas e qualidade adaptativa.
Documentação completa, configuração e comandos (`/astral ...`): veja [docs/ENGINE.md](docs/ENGINE.md).
