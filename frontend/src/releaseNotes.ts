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
