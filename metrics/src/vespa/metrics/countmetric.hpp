// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include "countmetric.h"
#include "memoryconsumption.h"
#include <sstream>

namespace metrics {

template <typename T, bool SumOnAdd>
CountMetric<T, SumOnAdd>::CountMetric(const String& name, const String& tags,
                                      const String& desc, MetricSet* owner)
    : AbstractCountMetric(name, tags, desc, owner),
      _values()
{
    _values.setFlag(LOG_IF_UNSET);
}

template <typename T, bool SumOnAdd>
CountMetric<T, SumOnAdd>::CountMetric(const String& name, Tags dimensions,
                                      const String& desc, MetricSet* owner)
    : AbstractCountMetric(name, std::move(dimensions), desc, owner),
      _values()
{
    _values.setFlag(LOG_IF_UNSET);
}

template <typename T, bool SumOnAdd>
CountMetric<T, SumOnAdd>::CountMetric(const CountMetric<T, SumOnAdd>& other,
                                      CopyType copyType, MetricSet* owner)
    : AbstractCountMetric(other, owner),
      _values(other._values, copyType == CLONE ? other._values.size() : 1)
{
}

template <typename T, bool SumOnAdd>
CountMetric<T, SumOnAdd>&
CountMetric<T, SumOnAdd>::operator+=(const CountMetric<T, SumOnAdd>& other)
{
    T otherValues(other.getValue());
    bool overflow;
    Values values;
    do {
        values = _values.getValues();
        overflow = (values._value + otherValues < values._value);
        values._value += otherValues;
    } while (!_values.setValues(values));
    if (overflow) {
        _values.reset();
        std::ostringstream ost;
        ost << "Overflow in metric " << getPath() << " op +=. Resetting it.";
        logWarning(ost.str().c_str());
    }
    return *this;
}

template <typename T, bool SumOnAdd>
CountMetric<T, SumOnAdd>&
CountMetric<T, SumOnAdd>::operator-=(const CountMetric<T, SumOnAdd>& other)
{
    T otherValues(other.getValue());
    bool underflow;
    Values values;
    do {
        values = _values.getValues();
        underflow = (values._value - otherValues > values._value);
        values._value -= otherValues;
    } while (!_values.setValues(values));
    if (underflow) {
        _values.reset();
        std::ostringstream ost;
        ost << "Underflow in metric " << getPath() << " op -=. Resetting it.";
        logWarning(ost.str().c_str());
    }
    return *this;
}

template <typename T, bool SumOnAdd>
void
CountMetric<T, SumOnAdd>::set(T value)
{
    Values values;
    values._value = value;
    while (!_values.setValues(values)) {}
}

template <typename T, bool SumOnAdd>
void
CountMetric<T, SumOnAdd>::inc(T value)
{
    bool overflow;
    Values values;
    do {
        values = _values.getValues();
        overflow = (values._value + value < values._value);
        values._value += value;
    } while (!_values.setValues(values));
    if (overflow) {
        _values.reset();
        std::ostringstream ost;
        ost << "Overflow in metric " << getPath() << ". Resetting it.";
        logWarning(ost.str().c_str());
    }
}

template <typename T, bool SumOnAdd>
void
CountMetric<T, SumOnAdd>::dec(T value)
{
    bool underflow;
    Values values;
    do {
        values = _values.getValues();
        underflow = (values._value - value > values._value);
        values._value -= value;
    } while (!_values.setValues(values));
    if (underflow) {
        _values.reset();
        std::ostringstream ost;
        ost << "Underflow in metric " << getPath() << ". Resetting it.";
        logWarning(ost.str().c_str());
    }
}

template <typename T, bool SumOnAdd>
void
CountMetric<T, SumOnAdd>::addToSnapshot(
        Metric& other, std::vector<Metric::LP>&) const
{
    CountMetric<T, SumOnAdd>& o(
            reinterpret_cast<CountMetric<T, SumOnAdd>&>(other));
    o.inc(_values.getValues()._value);
}

template <typename T, bool SumOnAdd>
void
CountMetric<T, SumOnAdd>::addToPart(Metric& other) const
{
    CountMetric<T, SumOnAdd>& o(
            reinterpret_cast<CountMetric<T, SumOnAdd>&>(other));
    if (SumOnAdd) {
        o.inc(_values.getValues()._value);
    } else {
        o.set((_values.getValues()._value + o._values.getValues()._value) / 2);
    }
}

template <typename T, bool SumOnAdd>
bool
CountMetric<T, SumOnAdd>::logEvent(const String& fullName) const
{
    Values values(_values.getValues());
    if (!logIfUnset() && values._value == 0) return false;
    sendLogCountEvent(
            fullName, static_cast<uint64_t>(values._value));
    return true;
}

template <typename T, bool SumOnAdd>
void
CountMetric<T, SumOnAdd>::print(std::ostream& out, bool verbose,
                                const std::string& indent,
                                uint64_t secondsPassed) const
{
    (void) indent;
    Values values(_values.getValues());
    if (values._value == 0 && !verbose) return;
    out << this->_name << (SumOnAdd ? " count=" : " value=") << values._value;
    if (SumOnAdd) {
        if (secondsPassed != 0) {
            double avgDiff = values._value / ((double) secondsPassed);
            out << " average_change_per_second=" << avgDiff;
        }
    }
}

template <typename T, bool SumOnAdd>
void
CountMetric<T, SumOnAdd>::addMemoryUsage(MemoryConsumption& mc) const
{
    ++mc._countMetricCount;
    mc._countMetricValues += _values.getMemoryUsageAllocatedInternally();
    mc._countMetricMeta += sizeof(CountMetric<T, SumOnAdd>)
                         - sizeof(Metric);
    Metric::addMemoryUsage(mc);
}

template <typename T, bool SumOnAdd>
void
CountMetric<T, SumOnAdd>::printDebug(std::ostream& out,
                                     const std::string& indent) const
{
    Values values(_values.getValues());
    out << "count=" << values._value << " ";
    Metric::printDebug(out, indent);
}

} // metrics

