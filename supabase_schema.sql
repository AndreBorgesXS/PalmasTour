-- Esquema do banco de dados Supabase para o aplicativo PalmasTour

-- Tabela para armazenar localizações
CREATE TABLE locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    name TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Habilitar RLS (Row Level Security) para a tabela locations
ALTER TABLE locations ENABLE ROW LEVEL SECURITY;

-- Criar política para permitir acesso anônimo de leitura e escrita
CREATE POLICY "Permitir acesso anônimo a locations" ON locations
    FOR ALL
    TO anon
    USING (true)
    WITH CHECK (true);

-- Tabela para armazenar fotos associadas a localizações
CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    file_path TEXT,
    storage_url TEXT,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Habilitar RLS (Row Level Security) para a tabela photos
ALTER TABLE photos ENABLE ROW LEVEL SECURITY;

-- Criar política para permitir acesso anônimo de leitura e escrita
CREATE POLICY "Permitir acesso anônimo a photos" ON photos
    FOR ALL
    TO anon
    USING (true)
    WITH CHECK (true);

-- Criar índice para melhorar a performance de consultas por location_id
CREATE INDEX idx_photos_location_id ON photos(location_id);

-- Configurar bucket de armazenamento para as fotos
INSERT INTO storage.buckets (id, name, public) VALUES ('photos', 'photos', true);

-- Criar política para permitir acesso anônimo ao bucket de fotos
CREATE POLICY "Permitir acesso anônimo ao bucket de fotos" ON storage.objects
    FOR ALL
    TO anon
    USING (bucket_id = 'photos')
    WITH CHECK (bucket_id = 'photos');