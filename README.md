# Assetum – Blockchain para Registo de Ativos Digitais

A Assetum é uma blockchain de tipo 1.0 desenvolvida com um objetivo pedagógico, centrada exclusivamente no registo e validação de ativos digitais únicos (assets) que representam bens do mundo real.

O sistema segue o modelo clássico de blockchain, baseado em uma cadeia de blocos encadeados por hashes e um mecanismo de consenso baseado em Prova de Trabalho (Proof-of-Work). No entanto, introduz uma simplificação didática ao substituir assinaturas digitais complexas por um sistema de pseudo-assinaturas baseadas em funções de hash, mantendo a integridade e autenticidade dos dados de forma mais acessível.

## Contexto Académico

Este projeto foi desenvolvido como trabalho académico integrado no 3.º ano, 5.º semestre da Licenciatura em Engenharia Informática, no âmbito da unidade curricular de Computação Distribuida. 

## Objetivos

- Implementar uma blockchain simplificada com fins educativos
- Representar ativos reais através de identificadores únicos
- Simular mecanismos de autenticação e consenso
- Facilitar a compreensão dos conceitos fundamentais de blockchain
- Explorar o registo seguro e imutável de dados


## Conceitos Principais

Blockchain - Estrutura de blocos encadeados através de hashes criptográficos, garantindo imutabilidade dos dados.

Proof-of-Work (PoW) - Mecanismo de consenso que exige esforço computacional para validação de novos blocos.

Pseudo-assinaturas - Substituição de assinaturas digitais tradicionais por funções de hash, garantindo autenticidade de forma simplificada.

## Estrutura dos Ativos (Assets) 

- Cada ativo registado na rede é identificado por um: assetId

Gerado a partir de atributos únicos do bem, como:
- Artigo matricial
- Freguesia
- Conservatória
- Ano
Este identificador garante a unicidade de cada ativo dentro da blockchain.

## Sistema de Utilizadores
Os utilizadores da rede são identificados por: ID único e Segredo privado (utilizado para autenticação simplificada).
A posse dos ativos é associada diretamente a estes utilizadores.

## Funcionamento Geral
1. Utilizador regista um ativo
2. gerado um assetId único
3. O ativo é incluído num bloco
4. O bloco é validado através de Proof-of-Work
5. O bloco é adicionado à blockchain
6. A posse do ativo fica associada ao utilizador


## Autores
- Juliana Mariana de Sousa Gaspar
- Inês Sapina Maciel
