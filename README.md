# AI Music Copilot Project

## Project Overview

This project is an AI assistant tool designed specifically for musicians. It integrates a rich knowledge base distilled from professional musicians’ experience, aiming to inspire creativity and provide expert advice in arrangement and mixing.

## Core Features

### 🎵 AI Music Creation Support

* Inspiration generation for music creation
* Professional arrangement suggestions
* Mixing techniques guidance
* Knowledge base built from professional musicians’ expertise

### 🤖 Multi-Model Integration

* Fast integration of multiple AI large models through the Spring AI framework
* Already supports mainstream models such as Tongyi and DeepSeek
* Provides a unified API interface for model calls

### 📚 Intelligent Document Processing (RAG)

* Uses `PagePdfDocumentReader` to process music knowledge documents
* Smart content chunking for documents
* Multimodal models convert document images into text
* Stores and retrieves data via Chroma vector database

### 🛠️ AI Tool Extensions

* Expands functionality using Spring AI tool-call annotations:

  * File operation tools
  * Resource download tools
  * Music clip generation tools
* Significantly extends the capabilities of the AI assistant

## Tech Stack

* Backend framework: Spring AI
* Vector database: Chroma

## Future Plans

* Integrate more music-specific models
* Extend supported document formats
* Enable local AI assistant operations within music production software
