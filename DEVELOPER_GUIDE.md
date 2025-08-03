# Guia do Desenvolvedor - PalmasTour

Este documento fornece orientações para desenvolvedores que desejam contribuir ou entender o funcionamento do aplicativo PalmasTour.

## Arquitetura do Projeto

O aplicativo segue uma arquitetura baseada em MVVM (Model-View-ViewModel):

- **Model**: Classes de dados como `Location` e `Photo`
- **View**: Fragmentos e layouts XML
- **ViewModel**: Classes como `HomeViewModel` que gerenciam o estado da UI

## Integração com Supabase

O aplicativo utiliza o Supabase como backend para armazenamento de dados. A comunicação é feita através da classe `SupabaseClient`, que encapsula as chamadas HTTP para a API do Supabase.

### Configuração do Supabase

1. Crie uma conta no [Supabase](https://supabase.com/)
2. Crie um novo projeto
3. Execute o script SQL fornecido no arquivo `supabase_schema.sql` no editor SQL do Supabase
4. Obtenha a URL e a chave anônima do projeto nas configurações
5. Atualize as strings `supabase_url` e `supabase_key` no arquivo `strings.xml`

### Estrutura do Banco de Dados

#### Tabela `locations`

| Coluna      | Tipo                     | Descrição                       |
|-------------|--------------------------|--------------------------------|
| id          | UUID                     | Identificador único (PK)       |
| latitude    | DOUBLE PRECISION         | Latitude da localização        |
| longitude   | DOUBLE PRECISION         | Longitude da localização       |
| name        | TEXT                     | Nome da localização (opcional) |
| created_at  | TIMESTAMP WITH TIME ZONE | Data de criação                |

#### Tabela `photos`

| Coluna      | Tipo                     | Descrição                       |
|-------------|--------------------------|--------------------------------|
| id          | UUID                     | Identificador único (PK)       |
| location_id | UUID                     | Referência à localização (FK)  |
| file_path   | TEXT                     | Caminho do arquivo local       |
| storage_url | TEXT                     | URL do arquivo no Storage      |
| description | TEXT                     | Descrição da foto (opcional)   |
| created_at  | TIMESTAMP WITH TIME ZONE | Data de criação                |

### Fluxo de Dados

1. O usuário captura uma localização e fotos no aplicativo
2. Ao salvar, o aplicativo:
   - Cria um registro na tabela `locations`
   - Faz upload das fotos para o bucket de armazenamento
   - Cria registros na tabela `photos` com referências à localização e URLs de armazenamento
3. Na visualização de galeria, o aplicativo consulta as localizações salvas
4. Na visualização de slideshow, o aplicativo consulta as fotos associadas a uma localização

## Permissões e Recursos

### Permissões do Android

O aplicativo requer as seguintes permissões:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.INTERNET" />
```

### Google Maps

O aplicativo utiliza o Google Maps para exibir a localização do usuário. Para configurar:

1. Obtenha uma chave de API do Google Maps no [Google Cloud Console](https://console.cloud.google.com/)
2. Ative as APIs Maps SDK for Android e Places API
3. Atualize a string `google_maps_key` no arquivo `strings.xml`

## Fluxo de Trabalho de Desenvolvimento

### Adicionando Novas Funcionalidades

1. Crie uma nova branch a partir da `main`
2. Implemente a funcionalidade
3. Teste em um dispositivo real ou emulador
4. Envie um pull request para revisão

### Depuração

Para depurar problemas de comunicação com o Supabase:

1. Verifique os logs do aplicativo para mensagens de erro
2. Verifique o painel do Supabase para erros de API
3. Use o editor SQL do Supabase para verificar se os dados estão sendo salvos corretamente

## Bibliotecas Principais

- **Google Maps**: Para exibição de mapas e localização
- **Supabase**: Para armazenamento de dados em nuvem
- **Glide**: Para carregamento e cache de imagens
- **Retrofit**: Para requisições HTTP
- **Gson**: Para conversão JSON
- **Coroutines**: Para operações assíncronas

## Melhorias Futuras

- Implementar autenticação de usuários
- Adicionar suporte para comentários em localizações
- Melhorar a interface do usuário com animações
- Adicionar suporte para compartilhamento de localizações
- Implementar cache offline para funcionamento sem internet