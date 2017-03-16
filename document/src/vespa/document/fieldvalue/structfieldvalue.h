// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
/**
 * \class document::StructFieldValue
 * \ingroup fieldvalue
 *
 * \brief Stores a set of predefined field <-> fieldvalue mappings.
 */
#pragma once

    // Not strictly needed to include exceptions.h, but to avoid clients needing
    // to to catch FieldNotFoundException
#include <vespa/document/datatype/structdatatype.h>
#include <vespa/document/fieldvalue/structuredfieldvalue.h>
#include <vespa/document/util/compressionconfig.h>
#include <vespa/document/fieldvalue/serializablearray.h>
#include <vector>

namespace document {
class Document;
class DocumentType;
class DocumentTypeRepo;
class FieldValueWriter;
class FixedTypeRepo;
class FieldSet;

class StructFieldValue : public StructuredFieldValue
{
public:
    class Chunks {
    public:
        Chunks() : _sz(0) { }
        ~Chunks();
        SerializableArray & operator [] (size_t i) { return *_chunks[i]; }
        const SerializableArray & operator [] (size_t i) const { return *_chunks[i]; }
        void push_back(SerializableArray::UP item) {
            assert(_sz < 2);
            _chunks[_sz++].reset(item.release());
        }
        SerializableArray & back() { return *_chunks[_sz-1]; }
        const SerializableArray & back() const { return *_chunks[_sz-1]; }
        size_t size() const { return _sz; }
        bool empty() const { return _sz == 0; }
        void clear() {
            _chunks[0].reset();
            _chunks[1].reset();
            _sz = 0;
        }
        void swap(Chunks & rhs) {
            _chunks[0].swap(rhs._chunks[0]);
            _chunks[1].swap(rhs._chunks[1]);
            std::swap(_sz, rhs._sz);
        }
    private:
        SerializableArray::CP _chunks[2];
        size_t _sz;
    };
private:
    Chunks   _chunks;

    // As we do lazy deserialization, we need these saved
    const DocumentTypeRepo *_repo;
    const DocumentType     *_doc_type;
    uint16_t                _version;
    mutable bool            _hasChanged;

public:
    typedef std::unique_ptr<StructFieldValue> UP;
    StructFieldValue(const DataType &type);
    ~StructFieldValue();
    void swap(StructFieldValue & rhs);

    void setRepo(const DocumentTypeRepo & repo) { _repo = & repo; }
    const DocumentTypeRepo * getRepo() const { return _repo; }
    void setDocumentType(const DocumentType & docType) { _doc_type = & docType; }

    const StructDataType & getStructType() const { return static_cast<const StructDataType &>(getType()); }

    void lazyDeserialize(const FixedTypeRepo &repo,
                         uint16_t version,
                         SerializableArray::EntryMap && fields,
                         ByteBuffer::UP buffer,
                         CompressionConfig::Type comp_type,
                         int32_t uncompressed_length);

    // returns false if the field could not be serialized.
    bool serializeField(int raw_field_id, uint16_t version, FieldValueWriter &writer) const;
    uint16_t getVersion() const { return _version; }

    const Chunks & getChunks() const {  return _chunks; }

    // raw_ids may contain ids for elements not in the struct's datatype.
    void getRawFieldIds(std::vector<int> &raw_ids) const;
    void getRawFieldIds(std::vector<int> &raw_ids, const FieldSet& fieldSet) const;

    void accept(FieldValueVisitor &visitor) override { visitor.visit(*this); }
    void accept(ConstFieldValueVisitor &visitor) const override { visitor.visit(*this); }

    bool hasField(const vespalib::stringref & name) const override;
    const Field& getField(const vespalib::stringref & name) const override;
    void clear() override;

    const CompressionConfig &getCompressionConfig() const
    { return getStructType().getCompressionConfig(); }

    // FieldValue implementation.
    FieldValue& assign(const FieldValue&) override;
    int compare(const FieldValue& other) const override;
    StructFieldValue* clone() const  override{
        return new StructFieldValue(*this);
    }

    void printXml(XmlOutputStream& out) const override;
    void print(std::ostream& out, bool verbose, const std::string& indent) const override;

    bool empty() const;

    bool hasChanged() const override { return _hasChanged; }

    uint32_t calculateChecksum() const;

    /**
     * Called by document to reset struct when deserializing where this struct
     * has no content. This clears content and sets changed to false.
     */
    void reset();

    DECLARE_IDENTIFIABLE_ABSTRACT(StructFieldValue);

private:
    void setFieldValue(const Field&, FieldValue::UP value) override;
    FieldValue::UP getFieldValue(const Field&) const override;
    bool getFieldValue(const Field&, FieldValue&) const override;
    bool hasFieldValue(const Field&) const override;
    void removeFieldValue(const Field&) override;
    VESPA_DLL_LOCAL vespalib::ConstBufferRef getRawField(uint32_t id) const;

    // Iterator implementation
    class FieldIterator;
    friend class FieldIterator;

    StructuredIterator::UP getIterator(const Field* toFind) const override;

    /** Called from Document when deserializing alters type. */
    void setType(const DataType& type) override;
    friend class Document; // Hide from others to prevent misuse
};

} // document

