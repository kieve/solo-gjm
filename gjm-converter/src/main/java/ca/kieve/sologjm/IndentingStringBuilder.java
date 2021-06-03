package ca.kieve.sologjm;

public class IndentingStringBuilder {
    private final StringBuilder m_stringBuilder;
    private final String m_indent;
    private int m_level;

    public IndentingStringBuilder(String indent) {
        m_stringBuilder = new StringBuilder();
        m_indent = indent;
        m_level = 0;
    }

    public IndentingStringBuilder push(String s) {
        li(s);
        m_level++;
        return this;
    }

    public IndentingStringBuilder pop(String s) {
        m_level--;
        li(s);
        return this;
    }

    public IndentingStringBuilder li(String s, Object... args) {
        m_stringBuilder.append(m_indent.repeat(m_level))
                .append(String.format(s, args))
                .append("\n");
        return this;
    }

    public IndentingStringBuilder li(String s) {
        m_stringBuilder.append(m_indent.repeat(m_level))
                .append(s)
                .append("\n");
        return this;
    }

    public IndentingStringBuilder s(String s) {
        m_stringBuilder.append(m_indent.repeat(m_level))
                .append(s);
        return this;
    }

    public IndentingStringBuilder a(String s) {
        m_stringBuilder.append(s);
        return this;
    }

    public IndentingStringBuilder a(int i) {
        m_stringBuilder.append(i);
        return this;
    }

    public IndentingStringBuilder e(String s) {
        m_stringBuilder.append(s)
                .append("\n");
        return this;
    }

    @Override
    public String toString() {
        return m_stringBuilder.toString();
    }
}
