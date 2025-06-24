-- Creating the users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    profile_picture VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Creating the produits table
CREATE TABLE IF NOT EXISTS produits (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    date_lancement TIMESTAMP,
    prix_des_concurrents DECIMAL(10, 2),
    cout_de_production DECIMAL(10, 2),
    desired_margin DECIMAL(10, 2),
    category VARCHAR(255),
    type VARCHAR(255),
    stock INTEGER,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_produit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Creating the optimal_prices table
CREATE TABLE IF NOT EXISTS optimal_prices (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    prix_des_concurrents DECIMAL(10, 2),
    optimal_price DECIMAL(10, 2) NOT NULL,
    potential_revenue DECIMAL(10, 2),
    margin DECIMAL(10, 2),
    user_id UUID NOT NULL,
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_optimal_price_product FOREIGN KEY (product_id) REFERENCES produits(id) ON DELETE CASCADE,
    CONSTRAINT fk_optimal_price_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Creating the tarification_prices table
CREATE TABLE IF NOT EXISTS tarification_prices (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    prix_des_concurrents DECIMAL(10, 2),
    tarification_price DECIMAL(10, 2) NOT NULL,
    potential_revenue DECIMAL(10, 2),
    margin DECIMAL(10, 2),
    user_id UUID NOT NULL,
    time_in_months DECIMAL(10, 2),
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tarification_price_product FOREIGN KEY (product_id) REFERENCES produits(id) ON DELETE CASCADE,
    CONSTRAINT fk_tarification_price_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);