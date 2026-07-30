--
-- PostgreSQL database dump
--

-- Dumped from database version 14.17 (Homebrew)
-- Dumped by pg_dump version 14.17 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


--
-- Name: citext; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;


--
-- Name: EXTENSION citext; Type: COMMENT; Schema: -; Owner: -
--



SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ducks_admin_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_admin_table (
    id bigint NOT NULL,
    name text NOT NULL,
    second_name text NOT NULL,
    login text NOT NULL,
    password text NOT NULL
);


--
-- Name: ducks_admin_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_admin_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_admin_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_admin_table_id_seq OWNED BY public.ducks_admin_table.id;


--
-- Name: ducks_coffee_constructor_categories_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_constructor_categories_table (
    id bigint NOT NULL,
    name text NOT NULL,
    shop_id bigint NOT NULL
);


--
-- Name: ducks_coffee_constructor_categories_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_constructor_categories_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_constructor_categories_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_constructor_categories_table_id_seq OWNED BY public.ducks_coffee_constructor_categories_table.id;


--
-- Name: ducks_coffee_constructors_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_constructors_table (
    id bigint NOT NULL,
    name text NOT NULL,
    price numeric(15,2),
    category_id bigint NOT NULL,
    shop_id bigint NOT NULL,
    is_in_stock boolean DEFAULT true NOT NULL
);


--
-- Name: ducks_coffee_constructors_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_constructors_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_constructors_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_constructors_table_id_seq OWNED BY public.ducks_coffee_constructors_table.id;


--
-- Name: ducks_coffee_modified_constructor_categories_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_modified_constructor_categories_table (
    id bigint NOT NULL,
    max_selection integer,
    min_selection integer,
    "categoryId" bigint NOT NULL,
    "defaultConstructorIds" jsonb
);


--
-- Name: ducks_coffee_modified_constructor_categories_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_modified_constructor_categories_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_modified_constructor_categories_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_modified_constructor_categories_table_id_seq OWNED BY public.ducks_coffee_modified_constructor_categories_table.id;


--
-- Name: ducks_coffee_ordered_products_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_ordered_products_table (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    product_name text NOT NULL,
    image_url text,
    constructors text,
    product_id bigint NOT NULL,
    minutes_to_cook integer,
    quantity integer NOT NULL,
    price numeric(15,2),
    selected_size json NOT NULL
);


--
-- Name: ducks_coffee_ordered_products_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_ordered_products_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_ordered_products_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_ordered_products_table_id_seq OWNED BY public.ducks_coffee_ordered_products_table.id;


--
-- Name: ducks_coffee_orders_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_orders_table (
    id bigint NOT NULL,
    coffee_shop_id bigint NOT NULL,
    created_timestamp bigint NOT NULL,
    accepted_timestamp bigint,
    finished_time bigint,
    estimated_finish_time bigint,
    user_id bigint NOT NULL,
    comment text,
    cancelled_message text,
    price numeric(15,2) NOT NULL,
    "isExpired" boolean DEFAULT false NOT NULL,
    "isCancelledBySeller" boolean DEFAULT false NOT NULL,
    "isCancelledByClient" boolean DEFAULT false NOT NULL,
    time_to_cook_in_minutes integer NOT NULL,
    tips numeric(15,2),
    total_price numeric(15,2) NOT NULL,
    is_to_time boolean DEFAULT false NOT NULL,
    ready_timestamp bigint,
    "isNotPickedUp" boolean DEFAULT false NOT NULL
);


--
-- Name: ducks_coffee_orders_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_orders_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_orders_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_orders_table_id_seq OWNED BY public.ducks_coffee_orders_table.id;


--
-- Name: ducks_coffee_product_category_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_product_category_table (
    id bigint NOT NULL,
    name text NOT NULL
);


--
-- Name: ducks_coffee_product_category_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_product_category_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_product_category_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_product_category_table_id_seq OWNED BY public.ducks_coffee_product_category_table.id;


--
-- Name: ducks_coffee_products_with_constructors_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_products_with_constructors_table (
    id bigint NOT NULL,
    constructor bigint NOT NULL,
    category bigint NOT NULL,
    product bigint NOT NULL
);


--
-- Name: ducks_coffee_products_with_constructors_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_products_with_constructors_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_products_with_constructors_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_products_with_constructors_table_id_seq OWNED BY public.ducks_coffee_products_with_constructors_table.id;


--
-- Name: ducks_coffee_shop_closest_delivery_time_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_shop_closest_delivery_time_table (
    id bigint NOT NULL,
    shop_id bigint NOT NULL,
    closest_time_in_minutes integer DEFAULT 5 NOT NULL
);


--
-- Name: ducks_coffee_shop_closest_delivery_time_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_shop_closest_delivery_time_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_shop_closest_delivery_time_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_shop_closest_delivery_time_table_id_seq OWNED BY public.ducks_coffee_shop_closest_delivery_time_table.id;


--
-- Name: ducks_coffee_shop_credentials_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_shop_credentials_table (
    id bigint NOT NULL,
    shop_id bigint NOT NULL,
    login text NOT NULL,
    password text NOT NULL,
    created_by bigint NOT NULL,
    pin_code text NOT NULL,
    pin_failed_attempts integer DEFAULT 0 NOT NULL,
    pin_last_failed_at bigint,
    pin_locked_until bigint
);


--
-- Name: ducks_coffee_shop_credentials_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_shop_credentials_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_shop_credentials_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_shop_credentials_table_id_seq OWNED BY public.ducks_coffee_shop_credentials_table.id;


--
-- Name: ducks_coffee_shop_product_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_shop_product_table (
    id bigint NOT NULL,
    name public.citext NOT NULL,
    description text,
    price numeric(10,2) NOT NULL,
    category_id bigint NOT NULL,
    shop_id bigint NOT NULL,
    sizes json NOT NULL,
    "imageUrl" text NOT NULL,
    carbohydrates numeric(6,2),
    protein numeric(6,2),
    fats numeric(6,2),
    calories numeric(7,2),
    in_stock boolean DEFAULT true NOT NULL,
    minutes_to_cook integer
);


--
-- Name: ducks_coffee_shop_product_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_shop_product_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_shop_product_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_shop_product_table_id_seq OWNED BY public.ducks_coffee_shop_product_table.id;


--
-- Name: ducks_coffee_shop_schedule_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_shop_schedule_table (
    id bigint NOT NULL,
    shop_id bigint NOT NULL,
    "dayOfWeek" integer NOT NULL,
    "startTime" text,
    "endTime" text,
    "isClosed" boolean DEFAULT false NOT NULL
);


--
-- Name: ducks_coffee_shop_schedule_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_shop_schedule_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_shop_schedule_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_shop_schedule_table_id_seq OWNED BY public.ducks_coffee_shop_schedule_table.id;


--
-- Name: ducks_coffee_shop_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_shop_table (
    id bigint NOT NULL,
    name public.citext NOT NULL,
    address public.citext NOT NULL,
    description text,
    "isShown" boolean DEFAULT false NOT NULL,
    tags jsonb,
    "photoUrls" jsonb,
    "lowestPrice" integer,
    "isTemporaryClosed" boolean DEFAULT false NOT NULL,
    closest_time_to_take_orders bigint,
    can_take_orders_reason integer,
    minutes_to_cook integer DEFAULT 2 NOT NULL,
    "tablesCapacity" integer DEFAULT 10 NOT NULL,
    "freeTables" integer DEFAULT 10 NOT NULL,
    "temporaryClosedReason" text,
    rating double precision NOT NULL,
    fcm_token text
);


--
-- Name: ducks_coffee_shop_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_shop_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_shop_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_shop_table_id_seq OWNED BY public.ducks_coffee_shop_table.id;


--
-- Name: ducks_coffee_shop_technical_pause_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_coffee_shop_technical_pause_table (
    id bigint NOT NULL,
    starts_at bigint NOT NULL,
    ends_at bigint NOT NULL,
    "coffeeShop" bigint NOT NULL,
    "isActive" boolean DEFAULT true NOT NULL
);


--
-- Name: ducks_coffee_shop_technical_pause_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_coffee_shop_technical_pause_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_coffee_shop_technical_pause_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_coffee_shop_technical_pause_table_id_seq OWNED BY public.ducks_coffee_shop_technical_pause_table.id;


--
-- Name: ducks_shop_credentials_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_shop_credentials_table (
    id bigint NOT NULL,
    shop_id bigint NOT NULL,
    login text NOT NULL,
    password text NOT NULL,
    created_by bigint NOT NULL
);


--
-- Name: ducks_shop_credentials_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_shop_credentials_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_shop_credentials_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_shop_credentials_table_id_seq OWNED BY public.ducks_shop_credentials_table.id;


--
-- Name: ducks_shop_product_colors_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_shop_product_colors_table (
    id integer NOT NULL,
    name text NOT NULL
);


--
-- Name: ducks_shop_product_colors_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_shop_product_colors_table_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_shop_product_colors_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_shop_product_colors_table_id_seq OWNED BY public.ducks_shop_product_colors_table.id;


--
-- Name: ducks_shop_product_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_shop_product_table (
    id bigint NOT NULL,
    shop bigint NOT NULL,
    name public.citext NOT NULL,
    description public.citext,
    brandname text,
    price numeric(15,2),
    category_id bigint NOT NULL,
    "photoUrls" jsonb NOT NULL,
    season_id integer,
    color integer,
    main_image_url text NOT NULL
);


--
-- Name: ducks_shop_product_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_shop_product_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_shop_product_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_shop_product_table_id_seq OWNED BY public.ducks_shop_product_table.id;


--
-- Name: ducks_shop_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_shop_table (
    id bigint NOT NULL,
    description text,
    "photoUrls" jsonb NOT NULL,
    name public.citext NOT NULL,
    address public.citext NOT NULL,
    tags jsonb
);


--
-- Name: ducks_shop_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_shop_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_shop_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_shop_table_id_seq OWNED BY public.ducks_shop_table.id;


--
-- Name: ducks_user_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ducks_user_table (
    id integer NOT NULL,
    name text,
    phone_number text NOT NULL,
    second_name text,
    fcm_token text
);


--
-- Name: ducks_user_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ducks_user_table_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ducks_user_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ducks_user_table_id_seq OWNED BY public.ducks_user_table.id;


--
-- Name: shop_product_category_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shop_product_category_table (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    "isSuperCategory" boolean DEFAULT false NOT NULL,
    "superCategoryId" bigint,
    parent_id bigint
);


--
-- Name: shop_product_category_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.shop_product_category_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shop_product_category_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.shop_product_category_table_id_seq OWNED BY public.shop_product_category_table.id;


--
-- Name: shop_product_size_table; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shop_product_size_table (
    id bigint NOT NULL,
    name text NOT NULL,
    "parentId" bigint
);


--
-- Name: shop_product_size_table_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.shop_product_size_table_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shop_product_size_table_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.shop_product_size_table_id_seq OWNED BY public.shop_product_size_table.id;


--
-- Name: shop_products_with_sizes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shop_products_with_sizes (
    id bigint NOT NULL,
    product bigint NOT NULL,
    size bigint NOT NULL
);


--
-- Name: shop_products_with_sizes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.shop_products_with_sizes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: shop_products_with_sizes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.shop_products_with_sizes_id_seq OWNED BY public.shop_products_with_sizes.id;


--
-- Name: ducks_admin_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_admin_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_admin_table_id_seq'::regclass);


--
-- Name: ducks_coffee_constructor_categories_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_constructor_categories_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_constructor_categories_table_id_seq'::regclass);


--
-- Name: ducks_coffee_constructors_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_constructors_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_constructors_table_id_seq'::regclass);


--
-- Name: ducks_coffee_modified_constructor_categories_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_modified_constructor_categories_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_modified_constructor_categories_table_id_seq'::regclass);


--
-- Name: ducks_coffee_ordered_products_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_ordered_products_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_ordered_products_table_id_seq'::regclass);


--
-- Name: ducks_coffee_orders_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_orders_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_orders_table_id_seq'::regclass);


--
-- Name: ducks_coffee_product_category_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_product_category_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_product_category_table_id_seq'::regclass);


--
-- Name: ducks_coffee_products_with_constructors_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_products_with_constructors_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_products_with_constructors_table_id_seq'::regclass);


--
-- Name: ducks_coffee_shop_closest_delivery_time_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_closest_delivery_time_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_shop_closest_delivery_time_table_id_seq'::regclass);


--
-- Name: ducks_coffee_shop_credentials_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_credentials_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_shop_credentials_table_id_seq'::regclass);


--
-- Name: ducks_coffee_shop_product_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_product_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_shop_product_table_id_seq'::regclass);


--
-- Name: ducks_coffee_shop_schedule_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_schedule_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_shop_schedule_table_id_seq'::regclass);


--
-- Name: ducks_coffee_shop_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_shop_table_id_seq'::regclass);


--
-- Name: ducks_coffee_shop_technical_pause_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_technical_pause_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_coffee_shop_technical_pause_table_id_seq'::regclass);


--
-- Name: ducks_shop_credentials_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_credentials_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_shop_credentials_table_id_seq'::regclass);


--
-- Name: ducks_shop_product_colors_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_product_colors_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_shop_product_colors_table_id_seq'::regclass);


--
-- Name: ducks_shop_product_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_product_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_shop_product_table_id_seq'::regclass);


--
-- Name: ducks_shop_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_shop_table_id_seq'::regclass);


--
-- Name: ducks_user_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_user_table ALTER COLUMN id SET DEFAULT nextval('public.ducks_user_table_id_seq'::regclass);


--
-- Name: shop_product_category_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_product_category_table ALTER COLUMN id SET DEFAULT nextval('public.shop_product_category_table_id_seq'::regclass);


--
-- Name: shop_product_size_table id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_product_size_table ALTER COLUMN id SET DEFAULT nextval('public.shop_product_size_table_id_seq'::regclass);


--
-- Name: shop_products_with_sizes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_products_with_sizes ALTER COLUMN id SET DEFAULT nextval('public.shop_products_with_sizes_id_seq'::regclass);


--
-- Name: ducks_admin_table ducks_admin_table_login_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_admin_table
    ADD CONSTRAINT ducks_admin_table_login_unique UNIQUE (login);


--
-- Name: ducks_admin_table ducks_admin_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_admin_table
    ADD CONSTRAINT ducks_admin_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_constructor_categories_table ducks_coffee_constructor_categories_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_constructor_categories_table
    ADD CONSTRAINT ducks_coffee_constructor_categories_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_constructors_table ducks_coffee_constructors_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_constructors_table
    ADD CONSTRAINT ducks_coffee_constructors_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_modified_constructor_categories_table ducks_coffee_modified_constructor_categories_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_modified_constructor_categories_table
    ADD CONSTRAINT ducks_coffee_modified_constructor_categories_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_ordered_products_table ducks_coffee_ordered_products_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_ordered_products_table
    ADD CONSTRAINT ducks_coffee_ordered_products_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_orders_table ducks_coffee_orders_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_orders_table
    ADD CONSTRAINT ducks_coffee_orders_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_product_category_table ducks_coffee_product_category_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_product_category_table
    ADD CONSTRAINT ducks_coffee_product_category_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_products_with_constructors_table ducks_coffee_products_with_constructors_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_products_with_constructors_table
    ADD CONSTRAINT ducks_coffee_products_with_constructors_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_products_with_constructors_table ducks_coffee_products_with_constructors_table_product_construct; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_products_with_constructors_table
    ADD CONSTRAINT ducks_coffee_products_with_constructors_table_product_construct UNIQUE (product, constructor);


--
-- Name: ducks_coffee_shop_closest_delivery_time_table ducks_coffee_shop_closest_delivery_time_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_closest_delivery_time_table
    ADD CONSTRAINT ducks_coffee_shop_closest_delivery_time_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_shop_credentials_table ducks_coffee_shop_credentials_table_login_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_credentials_table
    ADD CONSTRAINT ducks_coffee_shop_credentials_table_login_unique UNIQUE (login);


--
-- Name: ducks_coffee_shop_credentials_table ducks_coffee_shop_credentials_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_credentials_table
    ADD CONSTRAINT ducks_coffee_shop_credentials_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_shop_credentials_table ducks_coffee_shop_credentials_table_shop_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_credentials_table
    ADD CONSTRAINT ducks_coffee_shop_credentials_table_shop_id_unique UNIQUE (shop_id);


--
-- Name: ducks_coffee_shop_product_table ducks_coffee_shop_product_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_product_table
    ADD CONSTRAINT ducks_coffee_shop_product_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_shop_schedule_table ducks_coffee_shop_schedule_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_schedule_table
    ADD CONSTRAINT ducks_coffee_shop_schedule_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_shop_table ducks_coffee_shop_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_table
    ADD CONSTRAINT ducks_coffee_shop_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_shop_technical_pause_table ducks_coffee_shop_technical_pause_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_technical_pause_table
    ADD CONSTRAINT ducks_coffee_shop_technical_pause_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_shop_credentials_table ducks_shop_credentials_table_login_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_credentials_table
    ADD CONSTRAINT ducks_shop_credentials_table_login_unique UNIQUE (login);


--
-- Name: ducks_shop_credentials_table ducks_shop_credentials_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_credentials_table
    ADD CONSTRAINT ducks_shop_credentials_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_shop_credentials_table ducks_shop_credentials_table_shop_id_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_credentials_table
    ADD CONSTRAINT ducks_shop_credentials_table_shop_id_unique UNIQUE (shop_id);


--
-- Name: ducks_shop_product_colors_table ducks_shop_product_colors_table_name_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_product_colors_table
    ADD CONSTRAINT ducks_shop_product_colors_table_name_unique UNIQUE (name);


--
-- Name: ducks_shop_product_colors_table ducks_shop_product_colors_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_product_colors_table
    ADD CONSTRAINT ducks_shop_product_colors_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_shop_product_table ducks_shop_product_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_product_table
    ADD CONSTRAINT ducks_shop_product_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_shop_table ducks_shop_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_table
    ADD CONSTRAINT ducks_shop_table_pkey PRIMARY KEY (id);


--
-- Name: ducks_user_table ducks_user_table_phone_number_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_user_table
    ADD CONSTRAINT ducks_user_table_phone_number_unique UNIQUE (phone_number);


--
-- Name: ducks_user_table ducks_user_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_user_table
    ADD CONSTRAINT ducks_user_table_pkey PRIMARY KEY (id);


--
-- Name: shop_product_category_table shop_product_category_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_product_category_table
    ADD CONSTRAINT shop_product_category_table_pkey PRIMARY KEY (id);


--
-- Name: shop_product_size_table shop_product_size_table_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_product_size_table
    ADD CONSTRAINT shop_product_size_table_pkey PRIMARY KEY (id);


--
-- Name: shop_products_with_sizes shop_products_with_sizes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_products_with_sizes
    ADD CONSTRAINT shop_products_with_sizes_pkey PRIMARY KEY (id);


--
-- Name: ducks_coffee_constructor_categories_table fk_ducks_coffee_constructor_categories_table_shop_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_constructor_categories_table
    ADD CONSTRAINT fk_ducks_coffee_constructor_categories_table_shop_id__id FOREIGN KEY (shop_id) REFERENCES public.ducks_coffee_shop_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_coffee_constructors_table fk_ducks_coffee_constructors_table_category_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_constructors_table
    ADD CONSTRAINT fk_ducks_coffee_constructors_table_category_id__id FOREIGN KEY (category_id) REFERENCES public.ducks_coffee_constructor_categories_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_coffee_constructors_table fk_ducks_coffee_constructors_table_shop_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_constructors_table
    ADD CONSTRAINT fk_ducks_coffee_constructors_table_shop_id__id FOREIGN KEY (shop_id) REFERENCES public.ducks_coffee_shop_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_coffee_modified_constructor_categories_table fk_ducks_coffee_modified_constructor_categories_table_categoryi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_modified_constructor_categories_table
    ADD CONSTRAINT fk_ducks_coffee_modified_constructor_categories_table_categoryi FOREIGN KEY ("categoryId") REFERENCES public.ducks_coffee_constructor_categories_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_coffee_ordered_products_table fk_ducks_coffee_ordered_products_table_order_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_ordered_products_table
    ADD CONSTRAINT fk_ducks_coffee_ordered_products_table_order_id__id FOREIGN KEY (order_id) REFERENCES public.ducks_coffee_orders_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_coffee_orders_table fk_ducks_coffee_orders_table_coffee_shop_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_orders_table
    ADD CONSTRAINT fk_ducks_coffee_orders_table_coffee_shop_id__id FOREIGN KEY (coffee_shop_id) REFERENCES public.ducks_coffee_shop_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_coffee_orders_table fk_ducks_coffee_orders_table_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_orders_table
    ADD CONSTRAINT fk_ducks_coffee_orders_table_user_id__id FOREIGN KEY (user_id) REFERENCES public.ducks_user_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_coffee_products_with_constructors_table fk_ducks_coffee_products_with_constructors_table_category__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_products_with_constructors_table
    ADD CONSTRAINT fk_ducks_coffee_products_with_constructors_table_category__id FOREIGN KEY (category) REFERENCES public.ducks_coffee_modified_constructor_categories_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_coffee_products_with_constructors_table fk_ducks_coffee_products_with_constructors_table_constructor__i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_products_with_constructors_table
    ADD CONSTRAINT fk_ducks_coffee_products_with_constructors_table_constructor__i FOREIGN KEY (constructor) REFERENCES public.ducks_coffee_constructors_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_coffee_products_with_constructors_table fk_ducks_coffee_products_with_constructors_table_product__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_products_with_constructors_table
    ADD CONSTRAINT fk_ducks_coffee_products_with_constructors_table_product__id FOREIGN KEY (product) REFERENCES public.ducks_coffee_shop_product_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_coffee_shop_closest_delivery_time_table fk_ducks_coffee_shop_closest_delivery_time_table_shop_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_closest_delivery_time_table
    ADD CONSTRAINT fk_ducks_coffee_shop_closest_delivery_time_table_shop_id__id FOREIGN KEY (shop_id) REFERENCES public.ducks_coffee_shop_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_coffee_shop_credentials_table fk_ducks_coffee_shop_credentials_table_created_by__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_credentials_table
    ADD CONSTRAINT fk_ducks_coffee_shop_credentials_table_created_by__id FOREIGN KEY (created_by) REFERENCES public.ducks_admin_table(id) ON UPDATE RESTRICT ON DELETE SET NULL;


--
-- Name: ducks_coffee_shop_credentials_table fk_ducks_coffee_shop_credentials_table_shop_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_credentials_table
    ADD CONSTRAINT fk_ducks_coffee_shop_credentials_table_shop_id__id FOREIGN KEY (shop_id) REFERENCES public.ducks_coffee_shop_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_coffee_shop_product_table fk_ducks_coffee_shop_product_table_category_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_product_table
    ADD CONSTRAINT fk_ducks_coffee_shop_product_table_category_id__id FOREIGN KEY (category_id) REFERENCES public.ducks_coffee_product_category_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_coffee_shop_product_table fk_ducks_coffee_shop_product_table_shop_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_product_table
    ADD CONSTRAINT fk_ducks_coffee_shop_product_table_shop_id__id FOREIGN KEY (shop_id) REFERENCES public.ducks_coffee_shop_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_coffee_shop_schedule_table fk_ducks_coffee_shop_schedule_table_shop_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_schedule_table
    ADD CONSTRAINT fk_ducks_coffee_shop_schedule_table_shop_id__id FOREIGN KEY (shop_id) REFERENCES public.ducks_coffee_shop_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_coffee_shop_technical_pause_table fk_ducks_coffee_shop_technical_pause_table_coffeeshop__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_coffee_shop_technical_pause_table
    ADD CONSTRAINT fk_ducks_coffee_shop_technical_pause_table_coffeeshop__id FOREIGN KEY ("coffeeShop") REFERENCES public.ducks_coffee_shop_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_shop_credentials_table fk_ducks_shop_credentials_table_created_by__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_credentials_table
    ADD CONSTRAINT fk_ducks_shop_credentials_table_created_by__id FOREIGN KEY (created_by) REFERENCES public.ducks_admin_table(id) ON UPDATE RESTRICT ON DELETE SET NULL;


--
-- Name: ducks_shop_credentials_table fk_ducks_shop_credentials_table_shop_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_credentials_table
    ADD CONSTRAINT fk_ducks_shop_credentials_table_shop_id__id FOREIGN KEY (shop_id) REFERENCES public.ducks_shop_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: ducks_shop_product_table fk_ducks_shop_product_table_category_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_product_table
    ADD CONSTRAINT fk_ducks_shop_product_table_category_id__id FOREIGN KEY (category_id) REFERENCES public.shop_product_category_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_shop_product_table fk_ducks_shop_product_table_color__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_product_table
    ADD CONSTRAINT fk_ducks_shop_product_table_color__id FOREIGN KEY (color) REFERENCES public.ducks_shop_product_colors_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: ducks_shop_product_table fk_ducks_shop_product_table_shop__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ducks_shop_product_table
    ADD CONSTRAINT fk_ducks_shop_product_table_shop__id FOREIGN KEY (shop) REFERENCES public.ducks_shop_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: shop_product_category_table fk_shop_product_category_table_parent_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_product_category_table
    ADD CONSTRAINT fk_shop_product_category_table_parent_id__id FOREIGN KEY (parent_id) REFERENCES public.shop_product_category_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: shop_product_category_table fk_shop_product_category_table_supercategoryid__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_product_category_table
    ADD CONSTRAINT fk_shop_product_category_table_supercategoryid__id FOREIGN KEY ("superCategoryId") REFERENCES public.shop_product_category_table(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: shop_product_size_table fk_shop_product_size_table_parentid__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_product_size_table
    ADD CONSTRAINT fk_shop_product_size_table_parentid__id FOREIGN KEY ("parentId") REFERENCES public.shop_product_size_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: shop_products_with_sizes fk_shop_products_with_sizes_product__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_products_with_sizes
    ADD CONSTRAINT fk_shop_products_with_sizes_product__id FOREIGN KEY (product) REFERENCES public.ducks_shop_product_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: shop_products_with_sizes fk_shop_products_with_sizes_size__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shop_products_with_sizes
    ADD CONSTRAINT fk_shop_products_with_sizes_size__id FOREIGN KEY (size) REFERENCES public.shop_product_size_table(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

