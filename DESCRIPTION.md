O projeto **PalmasTour** é um aplicativo Android desenvolvido para **captura e registro de localizações turísticas** na cidade de Palmas-TO, oferecendo suporte para inclusão de fotos e utilizando armazenamento em nuvem.

### Funcionalidades Principais
O aplicativo permite aos usuários:
*   **Capturar a localização geográfica** utilizando GPS.
*   **Capturar fotos** diretamente com a câmera do dispositivo.
*   **Armazenar dados em nuvem** através do Supabase.
*   **Visualizar as localizações registradas em um mapa**, utilizando o Google Maps.
*   Acessar uma **galeria de fotos por localização**.

### Arquitetura e Estrutura
O PalmasTour segue uma **arquitetura MVVM (Model-View-ViewModel)**, que organiza o projeto da seguinte forma:
*   **Model**: Inclui classes de dados como `Location` e `Photo`.
*   **View**: Composta por fragmentos e layouts XML.
*   **ViewModel**: Contém classes como `HomeViewModel`, responsáveis por gerenciar o estado da interface do usuário.

A estrutura do projeto principal (`app/src/main/java/br/edu/ifto/pdm/palmastour/`) é organizada em:
*   `ui/home/`: Fragmento principal que exibe o mapa e as funcionalidades de captura.
*   `ui/gallery/`: Responsável pela visualização das localizações salvas.
*   `ui/slideshow/`: Exibe as fotos associadas a uma localização específica.
*   `data/`: Contém as classes de modelo (`Location.java`, `Photo.java`) e a camada de acesso a dados (`SupabaseClient.java`).

### Fluxo de Dados
O aplicativo gerencia os dados da seguinte forma:
1.  Quando o usuário captura uma localização e fotos, ao salvar, o aplicativo:
    *   Cria um registro na **tabela `locations`**.
    *   Realiza o **upload das fotos para um bucket de armazenamento** no Supabase.
    *   Cria registros na **tabela `photos`**, fazendo referência à localização correspondente e incluindo as URLs de armazenamento das imagens.
2.  Na visualização de galeria, o aplicativo consulta as localizações que foram salvas.
3.  Na visualização de slideshow, são consultadas as fotos associadas a uma determinada localização.

### Tecnologias e Bibliotecas Utilizadas
O aplicativo integra diversas bibliotecas e serviços para suas funcionalidades:
*   **Supabase**: Utilizado como backend para armazenamento de dados em nuvem, com a comunicação encapsulada pela classe `SupabaseClient`.
    *   O banco de dados do Supabase inclui as tabelas `locations` (com colunas como `id`, `latitude`, `longitude`, `name`, `created_at`) e `photos` (com `id`, `location_id`, `file_path`, `storage_url`, `description`, `created_at`), além de um bucket de armazenamento e políticas de segurança para acesso aos dados.
*   **Google Maps**: Essencial para a exibição de mapas e localização do usuário.
*   **Glide**: Usado para carregamento e cache de imagens.
*   **Retrofit**: Para realizar requisições HTTP.
*   **Gson**: Para conversão de dados JSON.
*   **Coroutines**: Para gerenciar operações assíncronas.

### Requisitos e Configuração para Desenvolvimento
Para configurar e executar o projeto, são necessários:
*   Android SDK 30 ou superior.
*   Uma conta no Supabase.
*   Uma chave de API do Google Maps.

Os passos de configuração incluem:
1.  Criar uma conta e um novo projeto no Supabase.
2.  Executar o script SQL `supabase_schema.sql` no editor SQL do Supabase para configurar as tabelas (`locations`, `photos`), o bucket de armazenamento e as políticas de segurança.
3.  Obter a URL e a chave anônima do projeto Supabase.
4.  Obter uma chave de API do Google Maps no Google Cloud Console e ativar as APIs "Maps SDK for Android" e "Places API".
5.  Atualizar as strings `supabase_url`, `supabase_key` e `google_maps_key` no arquivo `strings.xml` do projeto Android.
6.  O projeto deve ser clonado e aberto no Android Studio antes da execução.

### Permissões do Android
O aplicativo requer as seguintes permissões para operar corretamente:
*   Acesso à localização (fina e aproximada).
*   Acesso à câmera.
*   Acesso ao armazenamento externo (para salvar fotos).
*   Acesso à internet.

### Melhorias Futuras
Algumas melhorias futuras previstas para o projeto incluem:
*   Implementar autenticação de usuários.
*   Adicionar suporte para comentários em localizações.
*   Melhorar a interface do usuário com animações.
*   Adicionar suporte para compartilhamento de localizações.
*   Implementar cache offline para permitir funcionamento sem conexão à internet.

O projeto é distribuído sob a licença MIT.