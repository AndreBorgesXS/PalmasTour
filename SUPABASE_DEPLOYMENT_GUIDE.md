# Guia de Implantação do Projeto PalmasTour no Supabase

Este guia detalha os passos necessários para implantar o projeto PalmasTour no Supabase, cobrindo desde a criação do projeto até a integração com as APIs e a configuração de funções.

## 1. Criação do Projeto no Supabase

1.  Acesse o [Supabase Dashboard](https://app.supabase.com/) e faça login.
2.  Clique em `New project`.
3.  Preencha os detalhes do projeto:
    *   **Name:** `PalmasTour` (ou outro nome de sua preferência).
    *   **Organization:** Selecione sua organização.
    *   **Database Password:** Crie uma senha forte para o banco de dados. **Guarde esta senha em segurança!**
    *   **Region:** Escolha a região mais próxima de seus usuários para menor latência.
4.  Clique em `Create new project`.

## 2. Configuração do Banco de Dados (Opcional - se o schema não foi importado)

*   **Nota:** arquivo `supabase_schema.sql` no projeto. Importar o schema do banco de dados.

1.  No Supabase Dashboard, navegue até `SQL Editor`.
2.  Clique em `New query`.
3.  Copie o conteúdo do arquivo `supabase_schema.sql` do seu projeto local e cole-o no editor SQL do Supabase.
4.  Clique em `RUN` para executar as migrações e criar as tabelas e funções necessárias.

## 3. Configuração das APIs e Integração

Após a criação do projeto, você precisará obter as chaves de API e configurar o projeto Android para se comunicar com o Supabase.

### 3.1. Obtenção das Chaves de API

1.  No Supabase Dashboard, vá para `Project Settings` (ícone de engrenagem no canto inferior esquerdo).
2.  Clique em `API`.
3.  Você encontrará as seguintes informações importantes:
    *   **Project URL:** O URL da sua instância Supabase.
    *   **Anon Public Key:** A chave pública que pode ser usada por clientes para interagir com o Supabase.
    *   **Service Role Key (Secret):** A chave de função de serviço, que possui privilégios totais. **NUNCA exponha esta chave no código do cliente!**

### 3.2. Integração no Projeto Android

Você precisará adicionar as dependências do Supabase ao seu projeto Android e configurar o cliente Supabase com as chaves obtidas.

1.  **Adicione as dependências no `build.gradle.kts` (Module: app):**

    ```kotlin
    // ...
    dependencies {
        // ... outras dependências
        implementation("io.supabase:supabase-kt:x.y.z") // Verifique a versão mais recente
    }
    ```

2.  **Configure o cliente Supabase no seu código Android:**

    É uma boa prática armazenar as chaves de API em um local seguro, como no `local.properties` (que é ignorado pelo Git) ou em variáveis de ambiente, e acessá-las em tempo de execução.

    Exemplo de inicialização do cliente Supabase (em uma classe `Application` ou similar):

    ```kotlin
    // app/src/main/java/com/example/palmaseventos/PalmasTourApplication.kt
    package com.example.palmaseventos

    import android.app.Application
    import io.supabase.SupabaseClient
    import io.supabase.createSupabaseClient

    class PalmasTourApplication : Application() {

        lateinit var supabase: SupabaseClient

        override fun onCreate() {
            super.onCreate()

            // Obtenha as chaves de forma segura (ex: de um arquivo de configuração ou variáveis de ambiente)
            val supabaseUrl = "SEU_SUPABASE_URL"
            val supabaseKey = "SUA_ANON_PUBLIC_KEY"

            supabase = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            ) {
                // Configurações adicionais, se necessário
            }
        }
    }
    ```

    **Lembre-se de substituir `SEU_SUPABASE_URL` e `SUA_ANON_PUBLIC_KEY` pelas suas chaves reais.**

## 4. Configuração de Funções (Edge Functions)

Se o seu projeto utiliza Supabase Edge Functions (funções serverless), você precisará implantá-las.

1.  **Instale o Supabase CLI:**

    Se você ainda não tem o Supabase CLI instalado, siga as instruções em [Supabase CLI](https://supabase.com/docs/guides/cli).

2.  **Faça login no Supabase CLI:**

    ```bash
    supabase login
    ```

3.  **Vincule seu projeto local ao projeto Supabase:**

    Navegue até a raiz do seu projeto local e execute:

    ```bash
    supabase link --project-ref SEU_PROJECT_REF
    ```
    Você pode encontrar o `SEU_PROJECT_REF` no URL do seu projeto no Supabase Dashboard (ex: `https://app.supabase.com/project/SEU_PROJECT_REF/...`).

4.  **Implante suas funções:**

    Se você tiver funções no diretório `supabase/functions` (ou similar), você pode implantá-las com:

    ```bash
    supabase functions deploy --all
    ```
    Ou para uma função específica:

    ```bash
    supabase functions deploy nome_da_sua_funcao
    ```

## 5. Configurações Adicionais

### 5.1. Autenticação

Se o seu aplicativo utiliza autenticação, configure os provedores de autenticação (e-mail/senha, Google, etc.) no Supabase Dashboard em `Authentication` -> `Settings`.

### 5.2. Armazenamento (Storage)

Para o armazenamento de fotos, configure os buckets e as políticas de acesso no Supabase Dashboard em `Storage`.

### 5.3. Políticas de Segurança (Row Level Security - RLS)

É crucial configurar as políticas de RLS para suas tabelas para garantir que os usuários só possam acessar os dados aos quais têm permissão. Isso é feito no Supabase Dashboard em `Authentication` -> `Policies`.

---

Este guia fornece uma visão geral dos passos de implantação. Para detalhes mais aprofundados, consulte a [documentação oficial do Supabase](https://supabase.com/docs).