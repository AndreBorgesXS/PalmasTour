# PalmasTour

Aplicativo Android para captura e registro de localizações turísticas em Palmas-TO, com suporte para fotos e armazenamento em nuvem usando Supabase.

## Funcionalidades

- Captura de localização geográfica usando GPS
- Captura de fotos usando a câmera do dispositivo
- Armazenamento de dados em nuvem usando Supabase
- Visualização de localizações em mapa usando Google Maps
- Galeria de fotos por localização

## Requisitos

- Android SDK 30 ou superior
- Conta no Supabase para armazenamento de dados
- Chave de API do Google Maps

## Configuração do Supabase

O aplicativo utiliza o Supabase como backend para armazenamento de dados. Para configurar o banco de dados, execute o script SQL fornecido no arquivo `supabase_schema.sql` no seu projeto Supabase.

O esquema inclui:

1. Tabela `locations` para armazenar informações de localização
2. Tabela `photos` para armazenar informações sobre as fotos capturadas
3. Bucket de armazenamento para as imagens
4. Políticas de segurança para acesso aos dados

## Configuração do Aplicativo

1. Clone o repositório
2. Abra o projeto no Android Studio
3. Configure as chaves de API no arquivo `strings.xml`:
   - `supabase_url`: URL do seu projeto Supabase
   - `supabase_key`: Chave anônima do seu projeto Supabase
   - `google_maps_key`: Chave de API do Google Maps
4. Execute o aplicativo em um dispositivo ou emulador

## Estrutura do Projeto

- `app/src/main/java/br/edu/ifto/pdm/palmastour/`
  - `ui/home/`: Fragmento principal com mapa e funcionalidades de captura
  - `ui/gallery/`: Visualização de localizações salvas
  - `ui/slideshow/`: Visualização de fotos por localização
  - `data/`: Classes de modelo e acesso a dados
    - `Location.java`: Modelo para localizações
    - `Photo.java`: Modelo para fotos
    - `SupabaseClient.java`: Cliente para comunicação com o Supabase

## Permissões

O aplicativo requer as seguintes permissões:

- Acesso à localização (fina e aproximada)
- Acesso à câmera
- Acesso ao armazenamento externo (para salvar fotos)
- Acesso à internet

## Licença

Este projeto é distribuído sob a licença MIT.