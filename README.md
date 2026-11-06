# Astral Arcana (Forge 1.20.1)

Magia cinematográfica: o **Cajado Cósmico** tem 4 magias.

- **Segure o botão direito** para carregar (círculo mágico, zoom, barras de cinema), **solte** para lançar.
- **Shift + botão direito** troca de magia.

| Magia | O que faz |
|---|---|
| Chuva de Meteoros | Meteoros em chamas caem em diagonal, com rastro, onda de choque e tremor de tela |
| Singularidade | Buraco negro com disco de acreção, jatos e gravidade que puxa tudo; colapsa no fim |
| Raio Astral | Raio de energia que atravessa inimigos e explode no destino |
| Supernova | Explosão cósmica ao redor de você: flash branco, esferas e anéis de choque |

Receita: Estrela do Nether + 2 Varas de Blaze (vertical).

## Serpente Cósmica (chefe)
Ovo de geração no menu criativo, ou `/summon astralarcana:cosmic_serpent`.
Corpo de 36 segmentos que seguem a cabeça, ondulando, com espinhos, anéis de luz e estrelas na pele.
Ataques: voo em círculos, mergulho, sopro de raio astral e chuva de meteoros. Dropa Estrela do Nether.
Só a cabeça pode ser atingida; o corpo causa dano ao encostar.

## Shader de distorção de tela
Lente gravitacional ao redor do buraco negro (com anel de fótons), ondas de choque que ondulam a tela,
blur radial e aberração cromática nas explosões e ao carregar magias.
Não funciona com shaderpacks (Iris/OptiFine) ativos.

## Compilar
- **GitHub Actions**: faça push; o .jar aparece em *Actions → Build mod → Artifacts*.
  Para release: `git tag v1.0.0 && git push --tags`.
- Local: `gradle build` (Gradle 8.1.1, Java 17). O .jar sai em `build/libs/`.

Os meteoros e explosões respeitam a gamerule `mobGriefing`.
