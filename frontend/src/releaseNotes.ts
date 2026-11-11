export interface ReleaseNoteItem {
  title: string;
  text: string;
}

export interface ReleaseNotes {
  version: string;
  date: string;
  title: string;
  intro: string;
  news: ReleaseNoteItem[];
  improvements: ReleaseNoteItem[];
  fixes: ReleaseNoteItem[];
}

export const RELEASE_NOTES: ReleaseNotes[] = [
  {
    version: '1.2.0',
    date: '2026-11-10',
    title: 'Mesa de guerra',
    intro: 'Forasteiro, a mesa ficou mais perigosa e mais divertida. Afie a espada e confira.',
    news: [
      {
        title: 'Vários combates ao mesmo tempo',
        text: 'O mestre conduz mais de uma luta na mesma mesa, cada uma com o seu nome. Você acompanha a sua em destaque e vê um resumo das outras.',
      },
      {
        title: 'Aviso de sua vez',
        text: 'Quando chega o seu turno, um aviso aparece no meio da tela e o celular vibra.',
      },
      {
        title: 'Chegou reforço',
        text: 'Dá para entrar numa luta que já começou. Você rola a iniciativa ao fim do turno atual e toda a mesa vê "Fulano entrou na partida".',
      },
      {
        title: 'Monstros ocultos',
        text: 'O mestre pode esconder criaturas na ordem de iniciativa. Na vez delas, você só vê "Turno do mestre"… até ela se revelar.',
      },
      {
        title: 'Reações',
        text: 'Uma por rodada, fora do seu turno, como um ataque de oportunidade. As rolagens aparecem no chat marcadas como reação, e ela volta no início do seu próximo turno.',
      },
      {
        title: 'Desafios para a mesa inteira',
        text: 'Todo desafio é anunciado para todos. A CD fica em segredo para quem só assiste.',
      },
      {
        title: 'Ajudas e atrapalhos',
        text: 'Dê um dado bônus, um dado de penalidade, vantagem ou desvantagem no teste de outro jogador. Em combate, isso gasta a sua reação. Quem recebe pode recusar uma ajuda.',
      },
    ],
    improvements: [
      {
        title: 'Iniciativa pela ficha',
        text: 'A iniciativa soma a Destreza e o novo bônus de iniciativa da ficha, para talentos como Alerta.',
      },
      {
        title: 'Resultado confirmado',
        text: 'O resultado do desafio só vale depois que quem desafiou confirma, para dar tempo de chegarem os dados de ajuda.',
      },
      {
        title: 'Lista de combate mais legível',
        text: 'A lista de jogadores do combate ficou mais fácil de ler no celular.',
      },
    ],
    fixes: [
      {
        title: 'Ficha',
        text: 'Os campos de nível, dado de vida e dados disponíveis da ficha agora ficam alinhados.',
      },
    ],
  },
  {
    version: '1.1.0',
    date: '2026-11-08',
    title: 'A ficha completa',
    intro:
      'Forasteiro, a ficha do seu personagem ganhou tudo o que faltava para a jornada. Puxe uma cadeira e confira as novidades.',
    news: [
      {
        title: 'Ficha em abas',
        text: 'Personagem, Atributos, Habilidades, Equipamento, Magias e Rolagens, cada coisa no seu lugar. No celular, é só deslizar para o lado. Cada aba carrega só o que precisa, então a ficha abre mais rápido.',
      },
      {
        title: 'Inspiração e testes contra a morte',
        text: 'Marque quando o mestre te der inspiração e acompanhe os sucessos e as falhas contra a morte, com um botão para limpar tudo de uma vez.',
      },
      {
        title: 'Idiomas e proficiências',
        text: 'Anote os idiomas que o seu personagem fala, com sugestões dos mais comuns, e as proficiências em ferramentas, armas e armaduras.',
      },
      {
        title: 'Antecedente e personalidade',
        text: 'Antecedente, traços de personalidade, ideais, vínculos e defeitos ganharam espaço próprio na ficha.',
      },
      {
        title: 'Percepção passiva automática',
        text: 'A ficha calcula sozinha: 10 + modificador de Sabedoria + bônus de proficiência.',
      },
      {
        title: 'Outros deslocamentos',
        text: 'Voo, natação, escalada, escavação e bônus de caminhada, com a origem e a marcação de temporário.',
      },
      {
        title: 'Dados de vida',
        text: 'Cada classe tem o seu dado de vida. Role a vida ao subir de nível e gaste dados de vida para se recuperar. Na mesa, a rolagem aparece no chat.',
      },
      {
        title: 'Bolsa de moedas',
        text: 'Cobre, prata, electro, ouro e platina. As moedas iniciais são roladas uma única vez, fora da mesa. Durante a aventura, você acrescenta o que o mestre narrar.',
      },
      {
        title: 'Pacotes de equipamento',
        text: 'Escolha um dos sete pacotes clássicos, como o do Explorador ou o do Sacerdote, e os itens entram direto na ficha.',
      },
      {
        title: 'CA adicional',
        text: 'Magia de proteção ou item encantado? Some o bônus à sua CA direto na mesa.',
      },
      {
        title: 'Histórico de rolagens',
        text: 'As 50 rolagens mais recentes da ficha ficam guardadas, e o mestre da mesa pode consultá-las.',
      },
    ],
    improvements: [
      {
        title: 'Rolagem de atributos',
        text: 'Na criação da ficha, você tem três tentativas para rolar os seis atributos. Depois, fique com o resultado ou ajuste à mão.',
      },
      {
        title: 'Amigos no celular',
        text: 'A lista de amigos ficou mais fácil de ler na tela pequena.',
      },
    ],
    fixes: [],
  },
];

export const LATEST_RELEASE = RELEASE_NOTES[0];
